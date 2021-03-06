package xyz.redtorch.gateway.ctp.x64v6v3v19p1v

import org.slf4j.LoggerFactory
import xyz.redtorch.common.enumeration.ConnectionStatusEnum
import xyz.redtorch.common.sync.dto.CancelOrder
import xyz.redtorch.common.sync.dto.InsertOrder
import xyz.redtorch.common.sync.dto.Notice
import xyz.redtorch.common.sync.enumeration.InfoLevelEnum
import xyz.redtorch.common.trade.dto.*
import xyz.redtorch.common.trade.enumeration.*
import xyz.redtorch.common.utils.CommonUtils
import xyz.redtorch.gateway.ctp.x64v6v3v19p1v.api.*
import java.io.File
import java.io.IOException
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.collections.ArrayList

class TdSpi internal constructor(private val ctpGatewayImpl: CtpGatewayImpl) : CThostFtdcTraderSpi() {
    companion object {
        private val logger = LoggerFactory.getLogger(TdSpi::class.java)
    }

    private val tdHost = ctpGatewayImpl.gatewayAdapterCtpSetting.tdHost!!
    private val tdPort = ctpGatewayImpl.gatewayAdapterCtpSetting.tdPort!!
    private val brokerId = ctpGatewayImpl.gatewayAdapterCtpSetting.brokerId!!
    private val userId = ctpGatewayImpl.gatewayAdapterCtpSetting.userId!!
    private val password = ctpGatewayImpl.gatewayAdapterCtpSetting.password!!
    private val appId = ctpGatewayImpl.gatewayAdapterCtpSetting.appId!!
    private val authCode = ctpGatewayImpl.gatewayAdapterCtpSetting.authCode!!
    private val userProductInfo = ctpGatewayImpl.gatewayAdapterCtpSetting.userProductInfo!!
    private val logInfo = ctpGatewayImpl.logInfo
    private val gatewayId = ctpGatewayImpl.gatewayId
    private val eventService = ctpGatewayImpl.getEventService()

    private var investorName = ""
    private var positionMap = HashMap<String, Position>()
    private val orderIdToAdapterOrderIdMap = ConcurrentHashMap<String, String>(50000)
    private val orderIdToOrderRefMap = ConcurrentHashMap<String, String>(50000)
    private val orderIdToOriginalOrderIdMap = HashMap<String, String>()
    private val originalOrderIdToOrderIdMap = HashMap<String, String>()
    private val exchangeIdAndOrderSysIdToOrderIdMap = ConcurrentHashMap<String, String>(50000)
    private val orderIdToInsertOrderMap = HashMap<String, InsertOrder>()
    private val orderIdToOrderMap = ConcurrentHashMap<String, Order>(50000)
    private val insertOrderLock: Lock = ReentrantLock()
    private var intervalQueryThread: Thread? = null
    private var cThostFtdcTraderApi: CThostFtdcTraderApi? = null
    private var connectionStatus = ConnectionStatusEnum.Disconnected // ??????????????????
    private var loginStatus = false // ????????????

    private var instrumentQueried = false
    private var investorNameQueried = false
    private val random = Random()
    private val reqId = AtomicInteger(random.nextInt(1800) % (1800 - 200 + 1) + 200) // ???????????????

    var tradingDay: String? = null
        private set

    @Volatile
    private var orderRef = random.nextInt(1800) % (1800 - 200 + 1) + 200 // ????????????
    private var loginFailed = false // ????????????????????????????????????????????????
    private var frontId = 0 // ???????????????
    private var sessionId = 0 // ????????????

    private var orderCacheList = LinkedList<Order>() // ????????????????????????Order
    private var tradeCacheList = LinkedList<Trade>() // ????????????????????????Trade


    private fun startIntervalQuery() {
        if (intervalQueryThread != null) {
            logger.error("{}???????????????????????????,????????????", logInfo)
            stopQuery()
        }
        intervalQueryThread = Thread {
            Thread.currentThread().name = "CTP Gateway Interval Query Thread, $gatewayId ${System.currentTimeMillis()}"
            while (!Thread.currentThread().isInterrupted) {
                try {
                    if (cThostFtdcTraderApi == null) {
                        logger.error("{}???????????????????????????API???????????????,??????", logInfo)
                        break
                    }
                    if (loginStatus) {
                        queryAccount()
                        Thread.sleep(1250)
                        queryPosition()
                        Thread.sleep(1250)
                    } else {
                        logger.warn("{}????????????,????????????", logInfo)
                    }
                } catch (e: InterruptedException) {
                    logger.warn("{}??????????????????????????????????????????,????????????", logInfo)
                    break
                } catch (e: Exception) {
                    logger.error("{}??????????????????????????????", logInfo, e)
                }
            }
        }
        intervalQueryThread!!.start()
    }

    private fun stopQuery() {
        try {
            if (intervalQueryThread != null && !intervalQueryThread!!.isInterrupted) {
                intervalQueryThread!!.interrupt()
                intervalQueryThread = null
            }
        } catch (e: Exception) {
            logger.error(logInfo + "????????????????????????", e)
        }
    }

    fun connect() {
        if (isConnected || connectionStatus == ConnectionStatusEnum.Connecting) {
            logger.warn("{}????????????????????????????????????????????????????????????", logInfo)
            return
        }
        if (connectionStatus == ConnectionStatusEnum.Connected) {
            reqAuth()
            return
        }
        connectionStatus = ConnectionStatusEnum.Connecting
        loginStatus = false
        instrumentQueried = false
        investorNameQueried = false
        if (cThostFtdcTraderApi != null) {
            try {
                val cThostFtdcTraderApiForRelease: CThostFtdcTraderApi = cThostFtdcTraderApi!!
                cThostFtdcTraderApi = null
                cThostFtdcTraderApiForRelease.RegisterSpi(null)
                Thread {
                    Thread.currentThread().name = "GatewayId $gatewayId TD API Release Thread, Time ${System.currentTimeMillis()}"
                    try {
                        logger.warn("?????????????????????????????????")
                        cThostFtdcTraderApiForRelease.Release()
                        logger.warn("?????????????????????????????????")
                    } catch (t: Throwable) {
                        logger.error("???????????????????????????????????????", t)
                    }
                }.start()
                Thread.sleep(100)
            } catch (t: Throwable) {
                logger.warn("{}?????????????????????????????????", logInfo, t)
            }
        }
        logger.warn("{}???????????????????????????", logInfo)
        val envTmpDir = System.getProperty("java.io.tmpdir")
        val separator = File.separator
        val tempFilePath =
            "${envTmpDir}${separator}xyz${separator}redtorch${separator}gateway${separator}ctp${separator}jctpv6v3v19p1x64api${separator}CTP_FLOW_TEMP${separator}TD_${gatewayId}"
        val tempFile = File(tempFilePath)
        if (!tempFile.parentFile.exists()) {
            try {
                CommonUtils.forceMkdirParent(tempFile)
                logger.info("{}????????????????????????????????? {}", logInfo, tempFile.parentFile.absolutePath)
            } catch (e: IOException) {
                logger.error("{}???????????????????????????????????????{}", logInfo, tempFile.parentFile.absolutePath, e)
            }
        }
        logger.warn("{}?????????????????????????????????{}", logInfo, tempFile.parentFile.absolutePath)
        try {
            cThostFtdcTraderApi = CThostFtdcTraderApi.CreateFtdcTraderApi(tempFile.absolutePath)
            cThostFtdcTraderApi!!.RegisterSpi(this)
            cThostFtdcTraderApi!!.RegisterFront("tcp://$tdHost:$tdPort")
            cThostFtdcTraderApi!!.Init()
        } catch (t: Throwable) {
            logger.error("{}????????????????????????", logInfo, t)
        }
        Thread {
            try {
                Thread.sleep((60 * 1000).toLong())
                if (!(isConnected && investorNameQueried && instrumentQueried)) {
                    logger.error("{}????????????????????????,????????????", logInfo)
                    ctpGatewayImpl.disconnect()
                }
            } catch (t: Throwable) {
                logger.error("{}??????????????????????????????????????????", logInfo, t)
            }
        }.start()
    }

    fun disconnect() {
        try {
            stopQuery()
            if (cThostFtdcTraderApi != null && connectionStatus != ConnectionStatusEnum.Disconnecting) {
                logger.warn("{}???????????????????????????????????????", logInfo)
                loginStatus = false
                instrumentQueried = false
                investorNameQueried = false
                connectionStatus = ConnectionStatusEnum.Disconnecting
                try {
                    if (cThostFtdcTraderApi != null) {
                        val cThostFtdcTraderApiForRelease: CThostFtdcTraderApi = cThostFtdcTraderApi!!
                        cThostFtdcTraderApi = null
                        cThostFtdcTraderApiForRelease.RegisterSpi(null)
                        Thread {
                            Thread.currentThread().name = "GatewayId $gatewayId TD API Release Thread,Start Time ${System.currentTimeMillis()}"
                            try {
                                logger.warn("?????????????????????????????????")
                                cThostFtdcTraderApiForRelease.Release()
                                logger.warn("?????????????????????????????????")
                            } catch (t: Throwable) {
                                logger.error("???????????????????????????????????????", t)
                            }
                        }.start()
                    }
                    Thread.sleep(100)
                } catch (t: Throwable) {
                    logger.error("{}???????????????????????????????????????", logInfo, t)
                }
                connectionStatus = ConnectionStatusEnum.Disconnected
                logger.warn("{}???????????????????????????????????????", logInfo)
            } else {
                logger.warn("{}????????????????????????????????????????????????,????????????", logInfo)
            }
        } catch (t: Throwable) {
            logger.error("{}???????????????????????????????????????", logInfo, t)
        }
    }

    val isConnected: Boolean
        get() = connectionStatus == ConnectionStatusEnum.Connected && loginStatus

    private fun queryAccount() {
        if (cThostFtdcTraderApi == null) {
            logger.warn("{}???????????????????????????,??????????????????", logInfo)
            return
        }
        if (!loginStatus) {
            logger.warn("{}????????????????????????,??????????????????", logInfo)
            return
        }
        if (!instrumentQueried) {
            logger.warn("{}???????????????????????????????????????,??????????????????", logInfo)
            return
        }
        if (!investorNameQueried) {
            logger.warn("{}??????????????????????????????????????????,??????????????????", logInfo)
            return
        }
        try {
            cThostFtdcTraderApi!!.ReqQryTradingAccount(CThostFtdcQryTradingAccountField(), reqId.incrementAndGet())
        } catch (t: Throwable) {
            logger.error("{}??????????????????????????????", logInfo, t)
        }
    }

    private fun queryPosition() {
        if (cThostFtdcTraderApi == null) {
            logger.warn("{}???????????????????????????,??????????????????", logInfo)
            return
        }
        if (!loginStatus) {
            logger.warn("{}????????????????????????,??????????????????", logInfo)
            return
        }
        if (!instrumentQueried) {
            logger.warn("{}???????????????????????????????????????,??????????????????", logInfo)
            return
        }
        if (!investorNameQueried) {
            logger.warn("{}??????????????????????????????????????????,??????????????????", logInfo)
            return
        }
        try {
            val cThostFtdcQryInvestorPositionField = CThostFtdcQryInvestorPositionField()
            cThostFtdcQryInvestorPositionField.brokerID = brokerId
            cThostFtdcQryInvestorPositionField.investorID = userId
            cThostFtdcTraderApi!!.ReqQryInvestorPosition(cThostFtdcQryInvestorPositionField, reqId.incrementAndGet())
        } catch (t: Throwable) {
            logger.error("{}??????????????????????????????", logInfo, t)
        }
    }

    fun submitOrder(insertOrder: InsertOrder): String? {
        if (cThostFtdcTraderApi == null) {
            logger.warn("{}???????????????????????????,????????????", logInfo)
            return null
        }
        if (!loginStatus) {
            logger.warn("{}????????????????????????,????????????", logInfo)
            return null
        }
        val cThostFtdcInputOrderField = CThostFtdcInputOrderField()
        cThostFtdcInputOrderField.instrumentID = insertOrder.contract!!.symbol
        cThostFtdcInputOrderField.limitPrice = insertOrder.price
        cThostFtdcInputOrderField.volumeTotalOriginal = insertOrder.volume
        cThostFtdcInputOrderField.orderPriceType = CtpConstant.orderPriceTypeMap.getOrDefault(insertOrder.orderPriceType, '\u0000')
        cThostFtdcInputOrderField.direction = CtpConstant.directionMap.getOrDefault(insertOrder.direction, '\u0000')
        cThostFtdcInputOrderField.combOffsetFlag = CtpConstant.offsetFlagMap.getOrDefault(insertOrder.offsetFlag, '\u0000').toString()
        cThostFtdcInputOrderField.investorID = userId
        cThostFtdcInputOrderField.userID = userId
        cThostFtdcInputOrderField.brokerID = brokerId
        cThostFtdcInputOrderField.exchangeID = CtpConstant.exchangeMap.getOrDefault(insertOrder.contract!!.exchange, "")
        cThostFtdcInputOrderField.combHedgeFlag = CtpConstant.hedgeFlagMap[insertOrder.hedgeFlag].toString()
        cThostFtdcInputOrderField.contingentCondition = CtpConstant.contingentConditionMap[insertOrder.contingentCondition]!!
        cThostFtdcInputOrderField.forceCloseReason = CtpConstant.forceCloseReasonMap[insertOrder.forceCloseReason]!!
        cThostFtdcInputOrderField.isAutoSuspend = insertOrder.autoSuspend
        cThostFtdcInputOrderField.isSwapOrder = insertOrder.swapOrder
        cThostFtdcInputOrderField.minVolume = insertOrder.minVolume
        cThostFtdcInputOrderField.timeCondition = CtpConstant.timeConditionMap.getOrDefault(insertOrder.timeCondition, '\u0000')
        cThostFtdcInputOrderField.volumeCondition = CtpConstant.volumeConditionMap.getOrDefault(insertOrder.volumeCondition, '\u0000')
        cThostFtdcInputOrderField.stopPrice = insertOrder.stopPrice

        // ????????????????????????,???????????????,???????????????????????????,????????????????????????
        insertOrderLock.lock()
        return try {
            val orderRef = ++orderRef
            val adapterOrderId = frontId.toString() + "_" + sessionId + "_" + orderRef

            val orderId = "$gatewayId@$adapterOrderId"
            if (insertOrder.originOrderId.isNotBlank()) {
                orderIdToOriginalOrderIdMap[orderId] = insertOrder.originOrderId
                originalOrderIdToOrderIdMap[insertOrder.originOrderId] = orderId
            }
            orderIdToInsertOrderMap[orderId] = insertOrder
            orderIdToAdapterOrderIdMap[orderId] = adapterOrderId
            orderIdToOrderRefMap[orderId] = orderRef.toString().padStart(20)
            cThostFtdcInputOrderField.orderRef = orderRef.toString()
            logger.error(
                "{}????????????????????????->{InstrumentID:{},LimitPrice:{}, VolumeTotalOriginal:{}, OrderPriceType:{}, Direction:{}, CombOffsetFlag:{}, OrderRef:{}, InvestorID:{}, UsrID:{}, BrokerID:{}, ExchangeID:{},CombHedgeFlag:{},ContingentCondition:{},ForceCloseReason:{},IsAutoSuspend:{},IsSwapOrder:{},MinVolume:{},TimeCondition:{},VolumeCondition:{},StopPrice:{}}",  //
                logInfo,  //
                cThostFtdcInputOrderField.instrumentID,  //
                cThostFtdcInputOrderField.limitPrice,  //
                cThostFtdcInputOrderField.volumeTotalOriginal,  //
                cThostFtdcInputOrderField.orderPriceType,  //
                cThostFtdcInputOrderField.direction,  //
                cThostFtdcInputOrderField.combOffsetFlag,  //
                cThostFtdcInputOrderField.orderRef,  //
                cThostFtdcInputOrderField.investorID,  //
                cThostFtdcInputOrderField.userID,  //
                cThostFtdcInputOrderField.brokerID,  //
                cThostFtdcInputOrderField.exchangeID,  //
                cThostFtdcInputOrderField.combHedgeFlag,  //
                cThostFtdcInputOrderField.contingentCondition,  //
                cThostFtdcInputOrderField.forceCloseReason,  //
                cThostFtdcInputOrderField.isAutoSuspend,  //
                cThostFtdcInputOrderField.isSwapOrder,  //
                cThostFtdcInputOrderField.minVolume,  //
                cThostFtdcInputOrderField.timeCondition,  //
                cThostFtdcInputOrderField.volumeCondition,  //
                cThostFtdcInputOrderField.stopPrice
            )
            cThostFtdcTraderApi!!.ReqOrderInsert(cThostFtdcInputOrderField, reqId.incrementAndGet())
            orderId
        } catch (t: Throwable) {
            logger.error("{}????????????????????????", logInfo, t)
            null
        } finally {
            insertOrderLock.unlock()
        }
    }

    // ??????
    fun cancelOrder(cancelOrder: CancelOrder): Boolean {
        if (cThostFtdcTraderApi == null) {
            logger.warn("{}???????????????????????????,????????????", logInfo)
            return false
        }
        if (!loginStatus) {
            logger.warn("{}????????????????????????,????????????", logInfo)
            return false
        }
        if (cancelOrder.orderId.isBlank() && cancelOrder.originOrderId.isBlank()) {
            logger.error("{}????????????,????????????", logInfo)
            return false
        }
        var orderId: String? = cancelOrder.orderId
        if (orderId.isNullOrBlank()) {
            orderId = originalOrderIdToOrderIdMap[cancelOrder.originOrderId]
            if (orderId.isNullOrBlank()) {
                logger.error("{}???????????????????????????????????????,????????????", logInfo)
                return false
            }
        }
        return try {
            val cThostFtdcInputOrderActionField = CThostFtdcInputOrderActionField()
            if (orderIdToInsertOrderMap.containsKey(orderId)) {
                orderIdToInsertOrderMap[orderId]?.let {
                    cThostFtdcInputOrderActionField.instrumentID = it.contract!!.symbol
                    cThostFtdcInputOrderActionField.exchangeID = CtpConstant.exchangeMap.getOrDefault(it.contract!!.exchange, "")
                    cThostFtdcInputOrderActionField.orderRef = orderIdToOrderRefMap[orderId]
                    cThostFtdcInputOrderActionField.frontID = frontId
                    cThostFtdcInputOrderActionField.sessionID = sessionId
                    cThostFtdcInputOrderActionField.actionFlag = jctpv6v3v19p1x64apiConstants.THOST_FTDC_AF_Delete
                    cThostFtdcInputOrderActionField.brokerID = brokerId
                    cThostFtdcInputOrderActionField.investorID = userId
                    cThostFtdcInputOrderActionField.userID = userId
                    cThostFtdcInputOrderActionField.exchangeID = CtpConstant.exchangeMap.getOrDefault(it.contract!!.exchange, "")
                    cThostFtdcTraderApi!!.ReqOrderAction(cThostFtdcInputOrderActionField, reqId.incrementAndGet())
                }
                true
            } else if (orderIdToOrderMap.containsKey(orderId)) {
                orderIdToOrderMap[orderId]?.let {
                    cThostFtdcInputOrderActionField.instrumentID = it.contract.symbol
                    cThostFtdcInputOrderActionField.exchangeID = CtpConstant.exchangeMap.getOrDefault(it.contract.exchange, "")
                    cThostFtdcInputOrderActionField.orderRef = orderIdToOrderRefMap[orderId]
                    cThostFtdcInputOrderActionField.frontID = it.frontId
                    cThostFtdcInputOrderActionField.sessionID = it.sessionId
                    cThostFtdcInputOrderActionField.actionFlag = jctpv6v3v19p1x64apiConstants.THOST_FTDC_AF_Delete
                    cThostFtdcInputOrderActionField.brokerID = brokerId
                    cThostFtdcInputOrderActionField.investorID = userId
                    cThostFtdcInputOrderActionField.userID = userId
                    cThostFtdcInputOrderActionField.exchangeID = CtpConstant.exchangeMap.getOrDefault(it.contract.exchange, "")
                    cThostFtdcTraderApi!!.ReqOrderAction(cThostFtdcInputOrderActionField, reqId.incrementAndGet())
                }
                true
            } else {
                logger.error("{}????????????????????????????????????,????????????", logInfo)
                false
            }
        } catch (t: Throwable) {
            logger.error("{}????????????", logInfo, t)
            false
        }
    }

    private fun reqAuth() {
        if (loginFailed) {
            logger.warn("{}?????????????????????????????????,????????????,????????????", logInfo)
            return
        }
        if (cThostFtdcTraderApi == null) {
            logger.warn("{}?????????????????????????????????,???????????????????????????", logInfo)
            return
        }
        if (brokerId.isBlank()) {
            logger.error("{}BrokerID???????????????", logInfo)
            return
        }
        if (userId.isBlank()) {
            logger.error("{}UserId???????????????", logInfo)
            return
        }
        if (password.isBlank()) {
            logger.error("{}Password???????????????", logInfo)
            return
        }
        if (appId.isBlank()) {
            logger.error("{}AppId???????????????", logInfo)
            return
        }
        if (authCode.isBlank()) {
            logger.error("{}AuthCode???????????????", logInfo)
            return
        }
        try {
            val authenticateField = CThostFtdcReqAuthenticateField()
            authenticateField.appID = appId
            authenticateField.authCode = authCode
            authenticateField.brokerID = brokerId
            authenticateField.userProductInfo = userProductInfo
            authenticateField.userID = userId
            cThostFtdcTraderApi!!.ReqAuthenticate(authenticateField, reqId.incrementAndGet())
        } catch (t: Throwable) {
            logger.error("{}???????????????????????????", logInfo, t)
            ctpGatewayImpl.disconnect()
        }
    }


    override fun OnFrontConnected() {
        try {
            logger.warn("{}??????????????????????????????", logInfo)
            // ???????????????????????????
            connectionStatus = ConnectionStatusEnum.Connected
            reqAuth()
        } catch (t: Throwable) {
            logger.error("{}OnFrontConnected Exception", logInfo, t)
        }
    }

    override fun OnFrontDisconnected(nReason: Int) {
        try {
            logger.warn("{}??????????????????????????????, ??????:{}", logInfo, nReason)
            ctpGatewayImpl.disconnect()
        } catch (t: Throwable) {
            logger.error("{}OnFrontDisconnected Exception", logInfo, t)
        }
    }

    override fun OnHeartBeatWarning(nTimeLapse: Int) {
        logger.warn("{}????????????????????????, Time Lapse:{}", logInfo, nTimeLapse)
    }

    override fun OnRspAuthenticate(
        pRspAuthenticateField: CThostFtdcRspAuthenticateField?,
        pRspInfo: CThostFtdcRspInfoField?,
        nRequestID: Int,
        bIsLast: Boolean
    ) {
        try {
            if (pRspInfo != null) {
                if (pRspInfo.errorID == 0) {
                    logger.warn(logInfo + "?????????????????????????????????")
                    val reqUserLoginField = CThostFtdcReqUserLoginField()
                    reqUserLoginField.brokerID = brokerId
                    reqUserLoginField.userID = userId
                    reqUserLoginField.password = password
                    cThostFtdcTraderApi!!.ReqUserLogin(reqUserLoginField, reqId.incrementAndGet())
                } else {
                    logger.error("{}????????????????????????????????? ??????ID:{},????????????:{}", logInfo, pRspInfo.errorID, pRspInfo.errorMsg)
                    loginFailed = true

                    // ?????????????????????
                    if (pRspInfo.errorID == 63) {
                        ctpGatewayImpl.authErrorFlag = true
                    }
                }
            } else {
                loginFailed = true
                logger.error("{}?????????????????????????????????????????????,??????????????????", logInfo)
            }
        } catch (t: Throwable) {
            loginFailed = true
            logger.error("{}?????????????????????????????????????????????", logInfo, t)
        }
    }

    override fun OnRspUserLogin(
        pRspUserLogin: CThostFtdcRspUserLoginField?,
        pRspInfo: CThostFtdcRspInfoField?,
        nRequestID: Int,
        bIsLast: Boolean
    ) {
        try {
            if (pRspInfo != null) {
                if (pRspInfo.errorID == 0) {
                    if (pRspUserLogin != null) {
                        logger.warn(
                            "{}???????????????????????? TradingDay:{},SessionID:{},BrokerID:{},UserID:{}",
                            logInfo,
                            pRspUserLogin.tradingDay,
                            pRspUserLogin.sessionID,
                            pRspUserLogin.brokerID,
                            pRspUserLogin.userID
                        )
                        sessionId = pRspUserLogin.sessionID
                        frontId = pRspUserLogin.frontID
                        // ?????????????????????true
                        loginStatus = true
                        tradingDay = pRspUserLogin.tradingDay
                        logger.warn("{}????????????????????????????????????{}", logInfo, tradingDay)

                        // ???????????????
                        val settlementInfoConfirmField = CThostFtdcSettlementInfoConfirmField()
                        settlementInfoConfirmField.brokerID = brokerId
                        settlementInfoConfirmField.investorID = userId
                        cThostFtdcTraderApi!!.ReqSettlementInfoConfirm(settlementInfoConfirmField, reqId.incrementAndGet())
                    } else {
                        logger.error("{}??????????????????????????????????????????", logInfo)
                    }
                } else {
                    // ??????????????????
                    if (pRspInfo.errorID == 3) {
                        ctpGatewayImpl.authErrorFlag = true
                    }
                    logger.error("{}?????????????????????????????? ??????ID:{},????????????:{}", logInfo, pRspInfo.errorID, pRspInfo.errorMsg)
                    loginFailed = true
                }
            } else {
                logger.error("{}??????????????????????????????????????????", logInfo)
            }

        } catch (t: Throwable) {
            logger.error("{}????????????????????????????????????", logInfo, t)
            loginFailed = true
        }
    }

    override fun OnRspUserLogout(
        pUserLogout: CThostFtdcUserLogoutField?,
        pRspInfo: CThostFtdcRspInfoField?,
        nRequestID: Int,
        bIsLast: Boolean
    ) {
        try {
            if (pRspInfo != null && pRspInfo.errorID != 0) {
                logger.error("{}OnRspUserLogout!??????ID:{},????????????:{}", logInfo, pRspInfo.errorID, pRspInfo.errorMsg)
            } else {
                if (pUserLogout != null) {
                    logger.info("{}OnRspUserLogout!BrokerID:{},UserId:{}", logInfo, pUserLogout.brokerID, pUserLogout.userID)
                }
            }
        } catch (t: Throwable) {
            logger.error("{}????????????????????????????????????", logInfo, t)
        }
        loginStatus = false
    }

    override fun OnRspUserPasswordUpdate(
        pUserPasswordUpdate: CThostFtdcUserPasswordUpdateField?,
        pRspInfo: CThostFtdcRspInfoField?,
        nRequestID: Int,
        bIsLast: Boolean
    ) {

    }

    override fun OnRspTradingAccountPasswordUpdate(
        pTradingAccountPasswordUpdate: CThostFtdcTradingAccountPasswordUpdateField?,
        pRspInfo: CThostFtdcRspInfoField?,
        nRequestID: Int,
        bIsLast: Boolean
    ) {

    }

    override fun OnRspUserAuthMethod(
        pRspUserAuthMethod: CThostFtdcRspUserAuthMethodField?,
        pRspInfo: CThostFtdcRspInfoField?,
        nRequestID: Int,
        bIsLast: Boolean
    ) {

    }

    override fun OnRspGenUserCaptcha(
        pRspGenUserCaptcha: CThostFtdcRspGenUserCaptchaField?,
        pRspInfo: CThostFtdcRspInfoField?,
        nRequestID: Int,
        bIsLast: Boolean
    ) {

    }

    override fun OnRspGenUserText(
        pRspGenUserText: CThostFtdcRspGenUserTextField?,
        pRspInfo: CThostFtdcRspInfoField?,
        nRequestID: Int,
        bIsLast: Boolean
    ) {

    }

    // ????????????
    override fun OnRspOrderInsert(
        pInputOrder: CThostFtdcInputOrderField?,
        pRspInfo: CThostFtdcRspInfoField?,
        nRequestID: Int,
        bIsLast: Boolean
    ) {
        try {
            if (pInputOrder != null) {
                val symbol = pInputOrder.instrumentID

                // ????????????????????????,??????userID????????????ID
                val accountCode = userId
                // ???????????????????????????????????????CNY
                val accountId = "$accountCode@CNY@$gatewayId"
                val frontId = frontId
                val sessionId = sessionId
                val orderRef: String = pInputOrder.orderRef
                val adapterOrderId = "" + frontId + "_" + sessionId + "_" + orderRef.trim()
                val orderId = "$gatewayId@$adapterOrderId"
                val direction = CtpConstant.directionMapReverse[pInputOrder.direction] ?: DirectionEnum.Unknown
                val offsetFlag = CtpConstant.offsetMapReverse[pInputOrder.combOffsetFlag.toCharArray()[0]] ?: OffsetFlagEnum.Unknown
                val price = pInputOrder.limitPrice
                val totalVolume = pInputOrder.volumeTotalOriginal
                val tradedVolume = 0
                val orderStatus = OrderStatusEnum.Rejected
                val hedgeFlag = CtpConstant.hedgeFlagMapReverse.getOrDefault(pInputOrder.combHedgeFlag.toCharArray()[0], HedgeFlagEnum.Unknown)
                val contingentCondition = CtpConstant.contingentConditionMapReverse[pInputOrder.contingentCondition] ?: ContingentConditionEnum.Unknown
                val forceCloseReason = CtpConstant.forceCloseReasonMapReverse[pInputOrder.forceCloseReason] ?: ForceCloseReasonEnum.Unknown
                val timeCondition = CtpConstant.timeConditionMapReverse[pInputOrder.timeCondition] ?: TimeConditionEnum.Unknown
                val gtdDate = pInputOrder.gtdDate
                val autoSuspend = pInputOrder.isAutoSuspend
                val userForceClose = pInputOrder.userForceClose
                val swapOrder = pInputOrder.isSwapOrder
                val volumeCondition = CtpConstant.volumeConditionMapReverse[pInputOrder.volumeCondition] ?: VolumeConditionEnum.Unknown
                val orderPriceType = CtpConstant.orderPriceTypeMapReverse[pInputOrder.orderPriceType] ?: OrderPriceTypeEnum.Unknown
                val minVolume = pInputOrder.minVolume
                val stopPrice = pInputOrder.stopPrice
                val originalOrderId = orderIdToOriginalOrderIdMap.getOrDefault(orderId, "")
                val order = Order()

                order.accountId = accountId
                order.originOrderId = originalOrderId
                order.orderId = orderId
                order.adapterOrderId = adapterOrderId
                order.direction = direction
                order.offsetFlag = offsetFlag
                order.price = price
                order.totalVolume = totalVolume
                order.tradedVolume = tradedVolume
                order.orderStatus = orderStatus
                order.tradingDay = tradingDay ?: ""
                order.frontId = frontId
                order.sessionId = sessionId
                order.gatewayId = gatewayId
                order.hedgeFlag = hedgeFlag
                order.contingentCondition = contingentCondition
                order.forceCloseReason = forceCloseReason
                order.timeCondition = timeCondition
                order.gtdDate = gtdDate
                order.autoSuspend = autoSuspend
                order.volumeCondition = volumeCondition
                order.minVolume = minVolume
                order.stopPrice = stopPrice
                order.userForceClose = userForceClose
                order.swapOrder = swapOrder
                order.orderPriceType = orderPriceType

                if (pRspInfo != null && pRspInfo.errorMsg != null) {
                    order.statusMsg = pRspInfo.errorMsg
                }

                if (instrumentQueried && ctpGatewayImpl.contractMap.containsKey(symbol)) {
                    order.contract = ctpGatewayImpl.contractMap[symbol]!!
                    orderIdToOrderMap[order.orderId] = order
                    eventService.emit(order)
                } else {
                    val contract = Contract()
                    contract.symbol = symbol
                    order.contract = contract
                    orderCacheList.add(order)
                }
            } else {
                logger.error("{}????????????????????????????????????(OnRspOrderInsert)??????,?????????", logInfo)
            }
            if (pRspInfo != null) {
                logger.error(
                    "{}??????????????????????????????(OnRspOrderInsert) ??????ID:{},????????????:{}",
                    logInfo,
                    pRspInfo.errorID,
                    pRspInfo.errorMsg
                )
                if (instrumentQueried) {
                    val notice = Notice()
                    notice.info =
                        logInfo + "??????????????????????????????(OnRspOrderInsert) ??????ID:" + pRspInfo.errorID + ",????????????:" + pRspInfo.errorMsg
                    notice.infoLevel = InfoLevelEnum.ERROR
                    notice.timestamp = System.currentTimeMillis()
                    eventService.emit(notice)
                }
            } else {
                logger.error("{}????????????????????????????????????(OnRspOrderInsert)??????,??????????????????", logInfo)
            }
        } catch (t: Throwable) {
            logger.error("{}????????????????????????????????????(OnRspOrderInsert)??????", logInfo, t)
        }
    }

    override fun OnRspParkedOrderInsert(
        pParkedOrder: CThostFtdcParkedOrderField?,
        pRspInfo: CThostFtdcRspInfoField?,
        nRequestID: Int,
        bIsLast: Boolean
    ) {

    }

    override fun OnRspParkedOrderAction(
        pParkedOrderAction: CThostFtdcParkedOrderActionField?,
        pRspInfo: CThostFtdcRspInfoField?,
        nRequestID: Int,
        bIsLast: Boolean
    ) {

    }

    // ??????????????????
    override fun OnRspOrderAction(
        pInputOrderAction: CThostFtdcInputOrderActionField?,
        pRspInfo: CThostFtdcRspInfoField?,
        nRequestID: Int,
        bIsLast: Boolean
    ) {
        if (pRspInfo != null) {
            logger.error("{}??????????????????????????????(OnRspOrderAction) ??????ID:{},????????????:{}", logInfo, pRspInfo.errorID, pRspInfo.errorMsg)
            if (instrumentQueried) {
                val notice = Notice()
                notice.info =
                    logInfo + "??????????????????????????????(OnRspOrderAction) ??????ID:" + pRspInfo.errorID + ",????????????:" + pRspInfo.errorMsg
                notice.infoLevel = InfoLevelEnum.ERROR
                notice.timestamp = System.currentTimeMillis()
                eventService.emit(notice)
            }
        } else {
            logger.error("{}????????????????????????????????????(OnRspOrderAction)??????,???????????????", logInfo)
        }
    }

    override fun OnRspQueryMaxOrderVolume(
        pQueryMaxOrderVolume: CThostFtdcQueryMaxOrderVolumeField?,
        pRspInfo: CThostFtdcRspInfoField?,
        nRequestID: Int,
        bIsLast: Boolean
    ) {

    }

    override fun OnRspSettlementInfoConfirm(
        pSettlementInfoConfirm: CThostFtdcSettlementInfoConfirmField?,
        pRspInfo: CThostFtdcRspInfoField?,
        nRequestID: Int,
        bIsLast: Boolean
    ) {
        try {
            if (pRspInfo != null) {
                if (pRspInfo.errorID == 0) {
                    logger.warn("{}????????????????????????????????????", logInfo)
                } else {
                    logger.error("{}???????????????????????????????????? ??????ID:{},????????????:{}", logInfo, pRspInfo.errorID, pRspInfo.errorMsg)
                    ctpGatewayImpl.disconnect()
                    return
                }

                // ???????????????
                Thread.sleep(1000)
                logger.warn("{}???????????????????????????????????????", logInfo)
                val pQryInvestor = CThostFtdcQryInvestorField()
                pQryInvestor.investorID = userId
                pQryInvestor.brokerID = brokerId
                cThostFtdcTraderApi!!.ReqQryInvestor(pQryInvestor, reqId.addAndGet(1))
            }
        } catch (t: Throwable) {
            logger.error("{}?????????????????????????????????", logInfo, t)
            ctpGatewayImpl.disconnect()
        }
    }

    override fun OnRspRemoveParkedOrder(
        pRemoveParkedOrder: CThostFtdcRemoveParkedOrderField?,
        pRspInfo: CThostFtdcRspInfoField?,
        nRequestID: Int,
        bIsLast: Boolean
    ) {

    }

    override fun OnRspRemoveParkedOrderAction(
        pRemoveParkedOrderAction: CThostFtdcRemoveParkedOrderActionField?,
        pRspInfo: CThostFtdcRspInfoField?,
        nRequestID: Int,
        bIsLast: Boolean
    ) {

    }

    override fun OnRspExecOrderInsert(
        pInputExecOrder: CThostFtdcInputExecOrderField?,
        pRspInfo: CThostFtdcRspInfoField?,
        nRequestID: Int,
        bIsLast: Boolean
    ) {

    }

    override fun OnRspExecOrderAction(
        pInputExecOrderAction: CThostFtdcInputExecOrderActionField?,
        pRspInfo: CThostFtdcRspInfoField?,
        nRequestID: Int,
        bIsLast: Boolean
    ) {

    }

    override fun OnRspForQuoteInsert(
        pInputForQuote: CThostFtdcInputForQuoteField?,
        pRspInfo: CThostFtdcRspInfoField?,
        nRequestID: Int,
        bIsLast: Boolean
    ) {

    }

    override fun OnRspQuoteInsert(
        pInputQuote: CThostFtdcInputQuoteField?,
        pRspInfo: CThostFtdcRspInfoField?,
        nRequestID: Int,
        bIsLast: Boolean
    ) {

    }

    override fun OnRspQuoteAction(
        pInputQuoteAction: CThostFtdcInputQuoteActionField?,
        pRspInfo: CThostFtdcRspInfoField?,
        nRequestID: Int,
        bIsLast: Boolean
    ) {

    }

    override fun OnRspBatchOrderAction(
        pInputBatchOrderAction: CThostFtdcInputBatchOrderActionField?,
        pRspInfo: CThostFtdcRspInfoField?,
        nRequestID: Int,
        bIsLast: Boolean
    ) {

    }

    override fun OnRspOptionSelfCloseInsert(
        pInputOptionSelfClose: CThostFtdcInputOptionSelfCloseField?,
        pRspInfo: CThostFtdcRspInfoField?,
        nRequestID: Int,
        bIsLast: Boolean
    ) {

    }

    override fun OnRspOptionSelfCloseAction(
        pInputOptionSelfCloseAction: CThostFtdcInputOptionSelfCloseActionField?,
        pRspInfo: CThostFtdcRspInfoField?,
        nRequestID: Int,
        bIsLast: Boolean
    ) {

    }

    override fun OnRspCombActionInsert(
        pInputCombAction: CThostFtdcInputCombActionField?,
        pRspInfo: CThostFtdcRspInfoField?,
        nRequestID: Int,
        bIsLast: Boolean
    ) {

    }

    override fun OnRspQryOrder(
        pOrder: CThostFtdcOrderField?,
        pRspInfo: CThostFtdcRspInfoField?,
        nRequestID: Int,
        bIsLast: Boolean
    ) {

    }

    override fun OnRspQryTrade(
        pTrade: CThostFtdcTradeField?,
        pRspInfo: CThostFtdcRspInfoField?,
        nRequestID: Int,
        bIsLast: Boolean
    ) {

    }

    // ??????????????????
    override fun OnRspQryInvestorPosition(
        pInvestorPosition: CThostFtdcInvestorPositionField?,
        pRspInfo: CThostFtdcRspInfoField?,
        nRequestID: Int,
        bIsLast: Boolean
    ) {
        try {
            if (pInvestorPosition == null || pInvestorPosition.instrumentID.isBlank()) {
                return
            }
            val symbol = pInvestorPosition.instrumentID
            if (!(instrumentQueried && ctpGatewayImpl.contractMap.containsKey(symbol))) {
                logger.warn("{}???????????????????????????,???????????????????????????,??????{}", logInfo, symbol)
            } else {
                val contract = ctpGatewayImpl.contractMap[symbol]
                val uniformSymbol = symbol + "@" + contract!!.exchange

                // ????????????????????????,??????userID????????????ID
                val accountCode = userId
                // ???????????????????????????????????????
                val accountId = "$accountCode@CNY@$gatewayId"
                val direction = CtpConstant.positionDirectionMapReverse[pInvestorPosition.posiDirection]
                    ?: PositionDirectionEnum.Unknown
                val hedgeFlag = CtpConstant.hedgeFlagMapReverse[pInvestorPosition.hedgeFlag] ?: HedgeFlagEnum.Unknown
                // ??????????????????
                val positionId = "$uniformSymbol@$direction@$hedgeFlag@$accountId"
                val position: Position?
                if (positionMap.containsKey(positionId)) {
                    position = positionMap[positionId]
                } else {
                    position = Position()
                    positionMap[positionId] = position
                    position.contract = ctpGatewayImpl.contractMap[symbol]!!
                    position.positionDirection = CtpConstant.positionDirectionMapReverse[pInvestorPosition.posiDirection] ?: PositionDirectionEnum.Unknown
                    position.positionId = positionId
                    position.accountId = accountId
                    position.gatewayId = gatewayId
                    position.hedgeFlag = hedgeFlag
                }
                position!!.useMargin = position.useMargin + pInvestorPosition.useMargin
                position.exchangeMargin = position.exchangeMargin + pInvestorPosition.exchangeMargin
                position.positionProfit = position.positionProfit + pInvestorPosition.positionProfit

                // ???????????????
                val cost = position.price * position.position * position.contract.multiplier
                val openCost = position.openPrice * position.position * position.contract.multiplier

                // ????????????
                position.position = position.position + pInvestorPosition.position

                // ????????????????????????
                if (position.position != 0) {
                    position.price =
                        (cost + pInvestorPosition.positionCost) / (position.position * position.contract.multiplier)
                    position.openPrice =
                        (openCost + pInvestorPosition.openCost) / (position.position * position.contract.multiplier)
                }
                if (position.positionDirection == PositionDirectionEnum.Long) {
                    position.frozen = position.frozen + pInvestorPosition.shortFrozen
                } else {
                    position.frozen = position.frozen + pInvestorPosition.longFrozen
                }
                if (ExchangeEnum.INE == position.contract.exchange || ExchangeEnum.SHFE == position.contract.exchange) {
                    // ????????????????????????????????????????????????????????????????????????????????????,??????????????????
                    if (pInvestorPosition.ydPosition > 0 && pInvestorPosition.todayPosition == 0) {
                        position.ydPosition = position.ydPosition + pInvestorPosition.position
                        if (position.positionDirection == PositionDirectionEnum.Long) {
                            position.ydFrozen = position.ydFrozen + pInvestorPosition.shortFrozen
                        } else {
                            position.ydFrozen = position.ydFrozen + pInvestorPosition.longFrozen
                        }
                    } else {
                        position.tdPosition = position.tdPosition + pInvestorPosition.position
                        if (position.positionDirection == PositionDirectionEnum.Long) {
                            position.tdFrozen = position.tdFrozen + pInvestorPosition.shortFrozen
                        } else {
                            position.tdFrozen = position.tdFrozen + pInvestorPosition.longFrozen
                        }
                    }
                } else {
                    position.tdPosition = position.tdPosition + pInvestorPosition.todayPosition
                    position.ydPosition = position.position - position.tdPosition

                    // ?????????????????????
                    if (ExchangeEnum.CFFEX == position.contract.exchange) {
                        if (position.tdPosition > 0) {
                            if (position.tdPosition >= position.frozen) {
                                position.tdFrozen = position.frozen
                            } else {
                                position.tdFrozen = position.tdPosition
                                position.ydFrozen = position.frozen - position.tdPosition
                            }
                        } else {
                            position.ydFrozen = position.frozen
                        }
                    } else {
                        // ????????????????????????????????????????????????????????????
                        if (position.ydPosition > 0) {
                            if (position.ydPosition >= position.frozen) {
                                position.ydFrozen = position.frozen
                            } else {
                                position.ydFrozen = position.ydPosition
                                position.tdFrozen = position.frozen - position.ydPosition
                            }
                        } else {
                            position.tdFrozen = position.frozen
                        }
                    }
                }
            }


            // ????????????
            if (bIsLast) {
                for (tmpPosition in positionMap.values) {
                    if (tmpPosition.position != 0) {
                        tmpPosition.priceDiff =
                            tmpPosition.positionProfit / tmpPosition.contract.multiplier / tmpPosition.position
                        if (tmpPosition.positionDirection == PositionDirectionEnum.Long
                            || tmpPosition.position > 0 && tmpPosition.positionDirection == PositionDirectionEnum.Net
                        ) {

                            // ??????????????????
                            tmpPosition.lastPrice = tmpPosition.price + tmpPosition.priceDiff
                            // ????????????????????????
                            tmpPosition.openPriceDiff = tmpPosition.lastPrice - tmpPosition.openPrice
                            // ??????????????????
                            tmpPosition.openPositionProfit =
                                tmpPosition.openPriceDiff * tmpPosition.position * tmpPosition.contract.multiplier
                        } else if (tmpPosition.positionDirection == PositionDirectionEnum.Short
                            || tmpPosition.position < 0 && tmpPosition.positionDirection == PositionDirectionEnum.Net
                        ) {

                            // ??????????????????
                            tmpPosition.lastPrice = tmpPosition.price - tmpPosition.priceDiff
                            // ????????????????????????
                            tmpPosition.openPriceDiff = tmpPosition.openPrice - tmpPosition.lastPrice
                            // ??????????????????
                            tmpPosition.openPositionProfit =
                                tmpPosition.openPriceDiff * tmpPosition.position * tmpPosition.contract.multiplier
                        } else {
                            logger.error("{}???????????????????????????????????????????????????{}", logInfo, tmpPosition.toString())
                        }

                        // ???????????????????????????
                        tmpPosition.contractValue =
                            tmpPosition.lastPrice * tmpPosition.contract.multiplier * tmpPosition.position
                        if (tmpPosition.useMargin != 0.0) {
                            tmpPosition.positionProfitRatio = tmpPosition.positionProfit / tmpPosition.useMargin
                            tmpPosition.openPositionProfitRatio = tmpPosition.openPositionProfit / tmpPosition.useMargin
                        }
                    }
                    // ??????????????????
                    tmpPosition.localCreatedTimestamp = System.currentTimeMillis()
                    eventService.emit(tmpPosition)
                }
                // ????????????
                positionMap = HashMap()
            }
        } catch (t: Throwable) {
            logger.error("{}??????????????????????????????", logInfo, t)
            ctpGatewayImpl.disconnect()
        }
    }

    // ??????????????????
    override fun OnRspQryTradingAccount(
        pTradingAccount: CThostFtdcTradingAccountField?,
        pRspInfo: CThostFtdcRspInfoField?,
        nRequestID: Int,
        bIsLast: Boolean
    ) {
        try {
            if (pTradingAccount != null) {
                val accountCode = pTradingAccount.accountID
                var currency = pTradingAccount.currencyID
                if (currency.isBlank()) {
                    currency = "CNY"
                }
                val accountId = "$accountCode@$currency@$gatewayId"
                val account = Account()
                account.code = accountCode
                account.currency = CurrencyEnum.valueOf(currency)
                account.available = pTradingAccount.available
                account.closeProfit = pTradingAccount.closeProfit
                account.commission = pTradingAccount.commission
                account.gatewayId = gatewayId
                account.margin = pTradingAccount.currMargin
                account.positionProfit = pTradingAccount.positionProfit
                account.preBalance = pTradingAccount.preBalance
                account.accountId = accountId
                account.deposit = pTradingAccount.deposit
                account.withdraw = pTradingAccount.withdraw
                account.name = investorName
                account.balance = pTradingAccount.balance
                account.localCreatedTimestamp = System.currentTimeMillis()
                eventService.emit(account)
            }
        } catch (t: Throwable) {
            logger.error("{}??????????????????????????????", logInfo, t)
            ctpGatewayImpl.disconnect()
        }
    }

    override fun OnRspQryInvestor(
        pInvestor: CThostFtdcInvestorField?,
        pRspInfo: CThostFtdcRspInfoField?,
        nRequestID: Int,
        bIsLast: Boolean
    ) {
        try {
            if (pRspInfo != null && pRspInfo.errorID != 0) {
                logger.error("{}??????????????????????????? ??????ID:{},????????????:{}", logInfo, pRspInfo.errorID, pRspInfo.errorMsg)
                ctpGatewayImpl.disconnect()
            } else {
                if (pInvestor != null) {
                    investorName = pInvestor.investorName
                    logger.warn("{}???????????????????????????????????????:{}", logInfo, investorName)
                } else {
                    logger.error("{}???????????????????????????????????????", logInfo)
                }
            }
            if (bIsLast) {
                if (investorName.isBlank()) {
                    logger.warn("{}???????????????????????????????????????,????????????", logInfo)
                    ctpGatewayImpl.disconnect()
                }
                investorNameQueried = true
                // ???????????????
                Thread.sleep(1000)
                // ??????????????????
                logger.warn("{}????????????????????????????????????", logInfo)
                val cThostFtdcQryInstrumentField = CThostFtdcQryInstrumentField()
                cThostFtdcTraderApi!!.ReqQryInstrument(cThostFtdcQryInstrumentField, reqId.incrementAndGet())
            }
        } catch (t: Throwable) {
            logger.error("{}?????????????????????????????????", logInfo, t)
            ctpGatewayImpl.disconnect()
        }


    }

    override fun OnRspQryTradingCode(
        pTradingCode: CThostFtdcTradingCodeField?,
        pRspInfo: CThostFtdcRspInfoField?,
        nRequestID: Int,
        bIsLast: Boolean
    ) {

    }

    override fun OnRspQryInstrumentMarginRate(
        pInstrumentMarginRate: CThostFtdcInstrumentMarginRateField?,
        pRspInfo: CThostFtdcRspInfoField?,
        nRequestID: Int,
        bIsLast: Boolean
    ) {

    }

    override fun OnRspQryInstrumentCommissionRate(
        pInstrumentCommissionRate: CThostFtdcInstrumentCommissionRateField?,
        pRspInfo: CThostFtdcRspInfoField?,
        nRequestID: Int,
        bIsLast: Boolean
    ) {

    }

    override fun OnRspQryExchange(
        pExchange: CThostFtdcExchangeField?,
        pRspInfo: CThostFtdcRspInfoField?,
        nRequestID: Int,
        bIsLast: Boolean
    ) {

    }

    override fun OnRspQryProduct(
        pProduct: CThostFtdcProductField?,
        pRspInfo: CThostFtdcRspInfoField?,
        nRequestID: Int,
        bIsLast: Boolean
    ) {

    }

    override fun OnRspQryInstrument(
        pInstrument: CThostFtdcInstrumentField?,
        pRspInfo: CThostFtdcRspInfoField?,
        nRequestID: Int,
        bIsLast: Boolean
    ) {
        try {
            if (pInstrument != null) {
                val productClass =
                    CtpConstant.productTypeMapReverse[pInstrument.productClass] ?: ProductClassEnum.Unknown
                var filterFlag = false
                if (productClass == ProductClassEnum.Futures || productClass == ProductClassEnum.Options || productClass == ProductClassEnum.SpotOption) {
                    filterFlag = true
                }
                if (filterFlag) {
                    val contract = Contract()
                    contract.symbol = pInstrument.instrumentID
                    contract.exchange = CtpConstant.exchangeMapReverse[pInstrument.exchangeID] ?: ExchangeEnum.Unknown
                    contract.productClass = productClass
                    contract.uniformSymbol = contract.symbol + "@" + contract.exchange
                    contract.name = pInstrument.instrumentName
                    contract.fullName = pInstrument.instrumentName
                    contract.thirdPartyId = contract.symbol
                    if (pInstrument.volumeMultiple <= 0) {
                        contract.multiplier = 1.0
                    } else {
                        contract.multiplier = pInstrument.volumeMultiple.toDouble()
                    }
                    contract.priceTick = pInstrument.priceTick
                    contract.currency = CurrencyEnum.CNY // ???????????????
                    contract.lastTradeDateOrContractMonth = pInstrument.expireDate
                    contract.strikePrice = pInstrument.strikePrice
                    contract.optionsType =
                        CtpConstant.optionTypeMapReverse[pInstrument.optionsType] ?: OptionsTypeEnum.Unknown
                    if (pInstrument.underlyingInstrID != null) {
                        contract.underlyingSymbol = pInstrument.underlyingInstrID
                    }
                    contract.underlyingMultiplier = pInstrument.underlyingMultiple
                    contract.maxLimitOrderVolume = pInstrument.maxLimitOrderVolume
                    contract.maxMarketOrderVolume = pInstrument.maxMarketOrderVolume
                    contract.minLimitOrderVolume = pInstrument.minLimitOrderVolume
                    contract.minMarketOrderVolume = pInstrument.minMarketOrderVolume
                    contract.maxMarginSideAlgorithm = pInstrument.maxMarginSideAlgorithm == '1'
                    contract.longMarginRatio = pInstrument.longMarginRatio
                    contract.shortMarginRatio = pInstrument.shortMarginRatio
                    ctpGatewayImpl.contractMap[contract.symbol] = contract
                }
            }
            if (bIsLast) {
                if (ctpGatewayImpl.contractMap.isNotEmpty()) {
                    eventService.emitContractList(ArrayList(ctpGatewayImpl.contractMap.values))
                }
                logger.warn("{}????????????????????????????????????!??????{}???", logInfo, ctpGatewayImpl.contractMap.size)
                instrumentQueried = true
                startIntervalQuery()
                logger.warn("{}??????????????????????????????Order,??????{}???", logInfo, orderCacheList.size)

                if (orderCacheList.isNotEmpty()) {
                    val emitOrderList =ArrayList<Order>()
                    for (order in orderCacheList) {
                        ctpGatewayImpl.contractMap[order.contract.symbol].let {
                            if(it == null){
                                logger.error("{}??????Order??????,?????????????????????????????????,??????{}", logInfo, order.contract.symbol)
                            }else{
                                order.contract = it
                                emitOrderList.add(order)
                            }
                        }
                    }
                    if(emitOrderList.isNotEmpty()){
                        eventService.emitOrderList(emitOrderList)
                    }
                }
                orderCacheList = LinkedList<Order>()

                logger.warn("{}??????????????????????????????Trade,??????{}???", logInfo, tradeCacheList.size)
                if (tradeCacheList.isNotEmpty()) {
                    val emitTradeList = ArrayList<Trade>()
                    for (trade in tradeCacheList) {
                        ctpGatewayImpl.contractMap[trade.contract.symbol].let {
                            if(it == null){
                                logger.error("{}??????Trade??????,?????????????????????????????????,??????{}", logInfo, trade.contract.symbol)
                            }else{
                                trade.contract = it
                                emitTradeList.add(trade)
                            }
                        }
                    }
                    if(emitTradeList.isNotEmpty()){
                        eventService.emitTradeList(emitTradeList)
                    }
                }
                tradeCacheList = LinkedList<Trade>()
            }
        } catch (t: Throwable) {
            logger.error("{}OnRspQryInstrument Exception", logInfo, t)
        }
    }

    override fun OnRspQryDepthMarketData(
        pDepthMarketData: CThostFtdcDepthMarketDataField?,
        pRspInfo: CThostFtdcRspInfoField?,
        nRequestID: Int,
        bIsLast: Boolean
    ) {

    }

    override fun OnRspQrySettlementInfo(
        pSettlementInfo: CThostFtdcSettlementInfoField?,
        pRspInfo: CThostFtdcRspInfoField?,
        nRequestID: Int,
        bIsLast: Boolean
    ) {

    }

    override fun OnRspQryTransferBank(
        pTransferBank: CThostFtdcTransferBankField?,
        pRspInfo: CThostFtdcRspInfoField?,
        nRequestID: Int,
        bIsLast: Boolean
    ) {

    }

    override fun OnRspQryInvestorPositionDetail(
        pInvestorPositionDetail: CThostFtdcInvestorPositionDetailField?,
        pRspInfo: CThostFtdcRspInfoField?,
        nRequestID: Int,
        bIsLast: Boolean
    ) {

    }

    override fun OnRspQryNotice(
        pNotice: CThostFtdcNoticeField?,
        pRspInfo: CThostFtdcRspInfoField?,
        nRequestID: Int,
        bIsLast: Boolean
    ) {

    }

    override fun OnRspQrySettlementInfoConfirm(
        pSettlementInfoConfirm: CThostFtdcSettlementInfoConfirmField?,
        pRspInfo: CThostFtdcRspInfoField?,
        nRequestID: Int,
        bIsLast: Boolean
    ) {

    }

    override fun OnRspQryInvestorPositionCombineDetail(
        pInvestorPositionCombineDetail: CThostFtdcInvestorPositionCombineDetailField?,
        pRspInfo: CThostFtdcRspInfoField?,
        nRequestID: Int,
        bIsLast: Boolean
    ) {

    }

    override fun OnRspQryCFMMCTradingAccountKey(
        pCFMMCTradingAccountKey: CThostFtdcCFMMCTradingAccountKeyField?,
        pRspInfo: CThostFtdcRspInfoField?,
        nRequestID: Int,
        bIsLast: Boolean
    ) {

    }

    override fun OnRspQryEWarrantOffset(
        pEWarrantOffset: CThostFtdcEWarrantOffsetField?,
        pRspInfo: CThostFtdcRspInfoField?,
        nRequestID: Int,
        bIsLast: Boolean
    ) {

    }

    override fun OnRspQryInvestorProductGroupMargin(
        pInvestorProductGroupMargin: CThostFtdcInvestorProductGroupMarginField?,
        pRspInfo: CThostFtdcRspInfoField?,
        nRequestID: Int,
        bIsLast: Boolean
    ) {

    }

    override fun OnRspQryExchangeMarginRate(
        pExchangeMarginRate: CThostFtdcExchangeMarginRateField?,
        pRspInfo: CThostFtdcRspInfoField?,
        nRequestID: Int,
        bIsLast: Boolean
    ) {

    }

    override fun OnRspQryExchangeMarginRateAdjust(
        pExchangeMarginRateAdjust: CThostFtdcExchangeMarginRateAdjustField?,
        pRspInfo: CThostFtdcRspInfoField?,
        nRequestID: Int,
        bIsLast: Boolean
    ) {

    }

    override fun OnRspQryExchangeRate(
        pExchangeRate: CThostFtdcExchangeRateField?,
        pRspInfo: CThostFtdcRspInfoField?,
        nRequestID: Int,
        bIsLast: Boolean
    ) {

    }

    override fun OnRspQrySecAgentACIDMap(
        pSecAgentACIDMap: CThostFtdcSecAgentACIDMapField?,
        pRspInfo: CThostFtdcRspInfoField?,
        nRequestID: Int,
        bIsLast: Boolean
    ) {

    }

    override fun OnRspQryProductExchRate(
        pProductExchRate: CThostFtdcProductExchRateField?,
        pRspInfo: CThostFtdcRspInfoField?,
        nRequestID: Int,
        bIsLast: Boolean
    ) {

    }

    override fun OnRspQryProductGroup(
        pProductGroup: CThostFtdcProductGroupField?,
        pRspInfo: CThostFtdcRspInfoField?,
        nRequestID: Int,
        bIsLast: Boolean
    ) {

    }

    override fun OnRspQryMMInstrumentCommissionRate(
        pMMInstrumentCommissionRate: CThostFtdcMMInstrumentCommissionRateField?,
        pRspInfo: CThostFtdcRspInfoField?,
        nRequestID: Int,
        bIsLast: Boolean
    ) {

    }

    override fun OnRspQryMMOptionInstrCommRate(
        pMMOptionInstrCommRate: CThostFtdcMMOptionInstrCommRateField?,
        pRspInfo: CThostFtdcRspInfoField?,
        nRequestID: Int,
        bIsLast: Boolean
    ) {

    }

    override fun OnRspQryInstrumentOrderCommRate(
        pInstrumentOrderCommRate: CThostFtdcInstrumentOrderCommRateField?,
        pRspInfo: CThostFtdcRspInfoField?,
        nRequestID: Int,
        bIsLast: Boolean
    ) {

    }

    override fun OnRspQrySecAgentTradingAccount(
        pTradingAccount: CThostFtdcTradingAccountField?,
        pRspInfo: CThostFtdcRspInfoField?,
        nRequestID: Int,
        bIsLast: Boolean
    ) {

    }

    override fun OnRspQrySecAgentCheckMode(
        pSecAgentCheckMode: CThostFtdcSecAgentCheckModeField?,
        pRspInfo: CThostFtdcRspInfoField?,
        nRequestID: Int,
        bIsLast: Boolean
    ) {

    }

    override fun OnRspQrySecAgentTradeInfo(
        pSecAgentTradeInfo: CThostFtdcSecAgentTradeInfoField?,
        pRspInfo: CThostFtdcRspInfoField?,
        nRequestID: Int,
        bIsLast: Boolean
    ) {

    }

    override fun OnRspQryOptionInstrTradeCost(
        pOptionInstrTradeCost: CThostFtdcOptionInstrTradeCostField?,
        pRspInfo: CThostFtdcRspInfoField?,
        nRequestID: Int,
        bIsLast: Boolean
    ) {

    }

    override fun OnRspQryOptionInstrCommRate(
        pOptionInstrCommRate: CThostFtdcOptionInstrCommRateField?,
        pRspInfo: CThostFtdcRspInfoField?,
        nRequestID: Int,
        bIsLast: Boolean
    ) {

    }

    override fun OnRspQryExecOrder(
        pExecOrder: CThostFtdcExecOrderField?,
        pRspInfo: CThostFtdcRspInfoField?,
        nRequestID: Int,
        bIsLast: Boolean
    ) {

    }

    override fun OnRspQryForQuote(
        pForQuote: CThostFtdcForQuoteField?,
        pRspInfo: CThostFtdcRspInfoField?,
        nRequestID: Int,
        bIsLast: Boolean
    ) {

    }

    override fun OnRspQryQuote(
        pQuote: CThostFtdcQuoteField?,
        pRspInfo: CThostFtdcRspInfoField?,
        nRequestID: Int,
        bIsLast: Boolean
    ) {

    }

    override fun OnRspQryOptionSelfClose(
        pOptionSelfClose: CThostFtdcOptionSelfCloseField?,
        pRspInfo: CThostFtdcRspInfoField?,
        nRequestID: Int,
        bIsLast: Boolean
    ) {

    }

    override fun OnRspQryInvestUnit(
        pInvestUnit: CThostFtdcInvestUnitField?,
        pRspInfo: CThostFtdcRspInfoField?,
        nRequestID: Int,
        bIsLast: Boolean
    ) {

    }

    override fun OnRspQryCombInstrumentGuard(
        pCombInstrumentGuard: CThostFtdcCombInstrumentGuardField?,
        pRspInfo: CThostFtdcRspInfoField?,
        nRequestID: Int,
        bIsLast: Boolean
    ) {

    }

    override fun OnRspQryCombAction(
        pCombAction: CThostFtdcCombActionField?,
        pRspInfo: CThostFtdcRspInfoField?,
        nRequestID: Int,
        bIsLast: Boolean
    ) {

    }

    override fun OnRspQryTransferSerial(
        pTransferSerial: CThostFtdcTransferSerialField?,
        pRspInfo: CThostFtdcRspInfoField?,
        nRequestID: Int,
        bIsLast: Boolean
    ) {

    }

    override fun OnRspQryAccountregister(
        pAccountregister: CThostFtdcAccountregisterField?,
        pRspInfo: CThostFtdcRspInfoField?,
        nRequestID: Int,
        bIsLast: Boolean
    ) {

    }

    override fun OnRspError(pRspInfo: CThostFtdcRspInfoField?, nRequestID: Int, bIsLast: Boolean) {
        try {
            if (pRspInfo != null) {
                logger.error("{}????????????????????????!??????ID:{},????????????:{},??????ID:{}", logInfo, pRspInfo.errorID, pRspInfo.errorMsg, nRequestID)
                if (instrumentQueried) {
                    if (pRspInfo.errorID == 0) {
                        val notice = Notice()
                        notice.info =
                            "??????:" + ctpGatewayImpl.gatewayName + ",??????ID:" + gatewayId + ",????????????????????????:" + pRspInfo.errorMsg + ",??????ID:" + pRspInfo.errorID
                        notice.infoLevel = InfoLevelEnum.INFO
                        notice.timestamp = System.currentTimeMillis()
                        eventService.emit(notice)
                    } else {
                        val notice = Notice()
                        notice.info =
                            "??????:" + ctpGatewayImpl.gatewayName + ",??????ID:" + gatewayId + ",????????????????????????:" + pRspInfo.errorMsg + ",??????ID:" + pRspInfo.errorID
                        notice.infoLevel = InfoLevelEnum.ERROR
                        notice.timestamp = System.currentTimeMillis()
                        eventService.emit(notice)
                    }
                }
                // CTP??????????????????,??????
                if (pRspInfo.errorID == 90) {
                    ctpGatewayImpl.disconnect()
                }
            }
        } catch (t: Throwable) {
            logger.error("{}OnRspError Exception", logInfo, t)
        }
    }

    override fun OnRtnOrder(pOrder: CThostFtdcOrderField?) {
        try {
            if (pOrder != null) {

                val symbol = pOrder.instrumentID

                // ????????????????????????,??????userID????????????ID
                val accountCode = userId
                // ???????????????????????????????????????CNY
                val accountId = "$accountCode@CNY@$gatewayId"
                val frontId = pOrder.frontID
                val sessionId = pOrder.sessionID
                val orderRef: String = pOrder.orderRef
                val adapterOrderId = "" + frontId.toString() + "_" + sessionId + "_" + orderRef.trim()
                val orderId = "$gatewayId@$adapterOrderId"
                val exchangeAndOrderSysId = pOrder.exchangeID + "@" + pOrder.orderSysID
                exchangeIdAndOrderSysIdToOrderIdMap[exchangeAndOrderSysId] = orderId
                orderIdToOrderRefMap[orderId] = orderRef
                orderIdToAdapterOrderIdMap[orderId] = adapterOrderId
                val direction = CtpConstant.directionMapReverse[pOrder.direction] ?: DirectionEnum.Unknown
                val offsetFlag =
                    CtpConstant.offsetMapReverse[pOrder.combOffsetFlag.toCharArray()[0]] ?: OffsetFlagEnum.Unknown
                val price = pOrder.limitPrice
                val totalVolume = pOrder.volumeTotalOriginal
                val tradedVolume = pOrder.volumeTraded
                val orderStatus = CtpConstant.statusMapReverse[pOrder.orderStatus]
                val statusMsg = pOrder.statusMsg
                val orderDate = pOrder.insertDate
                val orderTime = pOrder.insertTime
                val cancelTime = pOrder.cancelTime
                val activeTime = pOrder.activeTime
                val updateTime = pOrder.updateTime
                val suspendTime = pOrder.suspendTime
                val hedgeFlag = CtpConstant.hedgeFlagMapReverse.getOrDefault(pOrder.combHedgeFlag.toCharArray()[0], HedgeFlagEnum.Unknown)
                val contingentCondition =
                    CtpConstant.contingentConditionMapReverse[pOrder.contingentCondition] ?: ContingentConditionEnum.Unknown
                val forceCloseReason =
                    CtpConstant.forceCloseReasonMapReverse[pOrder.forceCloseReason] ?: ForceCloseReasonEnum.Unknown
                val timeCondition = CtpConstant.timeConditionMapReverse[pOrder.timeCondition] ?: TimeConditionEnum.Unknown
                val userForceClose = pOrder.userForceClose
                val gtdDate = pOrder.gtdDate
                val autoSuspend = pOrder.isAutoSuspend
                val swapOrder = pOrder.isSwapOrder
                val volumeCondition =
                    CtpConstant.volumeConditionMapReverse[pOrder.volumeCondition] ?: VolumeConditionEnum.Unknown
                val orderPriceType =
                    CtpConstant.orderPriceTypeMapReverse[pOrder.orderPriceType] ?: OrderPriceTypeEnum.Unknown
                val minVolume = pOrder.minVolume
                val stopPrice = pOrder.stopPrice
                val orderLocalId: String = pOrder.orderLocalID
                val orderSysId: String = pOrder.orderSysID
                val sequenceNo = pOrder.sequenceNo.toString()
                val brokerOrderSeq = pOrder.brokerOrderSeq.toString()
                val originalOrderId = orderIdToOriginalOrderIdMap.getOrDefault(orderId, "")
                val orderSubmitStatus =
                    CtpConstant.orderSubmitStatusMapReverse[pOrder.orderSubmitStatus] ?: OrderSubmitStatusEnum.Unknown

                val order = Order()
                order.accountId = accountId
                order.activeTime = activeTime
                order.adapterOrderId = adapterOrderId
                order.cancelTime = cancelTime
                order.direction = direction
                order.frontId = frontId
                order.offsetFlag = offsetFlag
                order.orderDate = orderDate
                order.orderId = orderId
                order.orderStatus = orderStatus!!
                order.orderTime = orderTime
                order.originOrderId = originalOrderId
                order.price = price
                order.sessionId = sessionId
                order.totalVolume = totalVolume
                order.tradedVolume = tradedVolume
                order.tradingDay = tradingDay!!
                order.updateTime = updateTime
                order.statusMsg = statusMsg
                order.gatewayId = gatewayId
                order.hedgeFlag = hedgeFlag
                order.contingentCondition = contingentCondition
                order.forceCloseReason = forceCloseReason
                order.timeCondition = timeCondition
                order.gtdDate = gtdDate
                order.autoSuspend = autoSuspend
                order.volumeCondition = volumeCondition
                order.minVolume = minVolume
                order.stopPrice = stopPrice
                order.userForceClose = userForceClose
                order.swapOrder = swapOrder
                order.suspendTime = suspendTime
                order.orderLocalId = orderLocalId
                order.orderSysId = orderSysId
                order.sequenceNo = sequenceNo
                order.brokerOrderSeq = brokerOrderSeq
                order.orderPriceType = orderPriceType
                order.orderSubmitStatus = orderSubmitStatus
                if (instrumentQueried) {
                    if (ctpGatewayImpl.contractMap.containsKey(symbol)) {
                        order.contract = ctpGatewayImpl.contractMap[symbol]!!
                        orderIdToOrderMap[order.orderId] = order
                        eventService.emit(order)
                    } else {
                        logger.error("{}????????????????????????????????????,???????????????:{}", logInfo, symbol)
                    }
                } else {
                    val contract = Contract()
                    contract.symbol = symbol
                    order.contract = contract
                    orderCacheList.add(order)
                }

            }
        } catch (t: Throwable) {
            logger.error("{}OnRtnOrder Exception", logInfo, t)
        }
    }

    override fun OnRtnTrade(pTrade: CThostFtdcTradeField?) {
        try {
            if (pTrade != null) {

                val exchangeAndOrderSysId = pTrade.exchangeID + "@" + pTrade.orderSysID
                val orderId = exchangeIdAndOrderSysIdToOrderIdMap.getOrDefault(exchangeAndOrderSysId, "")
                val adapterOrderId = orderIdToAdapterOrderIdMap.getOrDefault(orderId, "")
                val symbol = pTrade.instrumentID
                val direction = CtpConstant.directionMapReverse[pTrade.direction] ?: DirectionEnum.Unknown
                val adapterTradeId = adapterOrderId + "@" + direction + "@" + pTrade.tradeID.trim()
                val tradeId = "$gatewayId@$adapterTradeId"
                val offsetFlag = CtpConstant.offsetMapReverse[pTrade.offsetFlag] ?: OffsetFlagEnum.Unknown
                val price = pTrade.price
                val volume = pTrade.volume
                val tradeDate = pTrade.tradeDate
                val tradeTime = pTrade.tradeTime
                val hedgeFlag =
                    CtpConstant.hedgeFlagMapReverse.getOrDefault(pTrade.hedgeFlag, HedgeFlagEnum.Unknown)
                val tradeType = CtpConstant.tradeTypeMapReverse[pTrade.tradeType] ?: TradeTypeEnum.Unknown
                val priceSource = CtpConstant.priceSourceMapReverse[pTrade.priceSource] ?: PriceSourceEnum.Unknown
                val orderLocalId = pTrade.orderLocalID
                val orderSysId = pTrade.orderSysID
                val sequenceNo = pTrade.sequenceNo.toString()
                val brokerOrderSeq = pTrade.brokerOrderSeq.toString()
                val settlementID = pTrade.settlementID.toString()
                val originalOrderId = orderIdToOriginalOrderIdMap.getOrDefault(orderId, "")

                // ????????????????????????,??????userID????????????ID
                val accountCode = userId
                // ???????????????????????????????????????CNY
                val accountId = "$accountCode@CNY@$gatewayId"
                val trade = Trade()
                trade.accountId = accountId
                trade.adapterOrderId = adapterOrderId
                trade.adapterTradeId = adapterTradeId
                trade.tradeDate = tradeDate
                trade.tradeId = tradeId
                trade.tradeTime = tradeTime
                trade.tradingDay = tradingDay!!
                trade.direction = direction
                trade.offsetFlag = offsetFlag
                trade.orderId = orderId
                trade.originOrderId = originalOrderId
                trade.price = price
                trade.volume = volume
                trade.gatewayId = gatewayId
                trade.orderLocalId = orderLocalId
                trade.orderSysId = orderSysId
                trade.sequenceNo = sequenceNo
                trade.brokerOrderSeq = brokerOrderSeq
                trade.settlementId = settlementID
                trade.hedgeFlag = hedgeFlag
                trade.tradeType = tradeType
                trade.priceSource = priceSource
                if (instrumentQueried && ctpGatewayImpl.contractMap.containsKey(symbol)) {
                    trade.contract = ctpGatewayImpl.contractMap[symbol]!!
                    eventService.emit(trade)
                } else {
                    val contract = Contract()
                    contract.symbol = symbol
                    trade.contract = contract
                    tradeCacheList.add(trade)
                }

            }
        } catch (t: Throwable) {
            logger.error("{}OnRtnTrade Exception", logInfo, t)
        }
    }

    override fun OnErrRtnOrderInsert(pInputOrder: CThostFtdcInputOrderField?, pRspInfo: CThostFtdcRspInfoField?) {
        try {
            if (pRspInfo != null) {
                logger.error(
                    "{}?????????????????????????????????OnErrRtnOrderInsert??? ??????ID:{},????????????:{}",
                    logInfo,
                    pRspInfo.errorID,
                    pRspInfo.errorMsg
                )

                if (instrumentQueried) {
                    val notice = Notice()
                    notice.info =
                        logInfo + "?????????????????????????????????OnErrRtnOrderInsert??? ??????ID:" + pRspInfo.errorID + ",????????????:" + pRspInfo.errorMsg
                    notice.infoLevel = InfoLevelEnum.ERROR
                    notice.timestamp = System.currentTimeMillis()
                    eventService.emit(notice)
                }
            }
            if (pInputOrder != null) {
                logger.error(
                    "{}?????????????????????????????????OnErrRtnOrderInsert??? ?????????????????? ->{InstrumentID:{}, LimitPrice:{}, VolumeTotalOriginal:{}, OrderPriceType:{}, Direction:{}, CombOffsetFlag:{}, OrderRef:{}, InvestorID:{}, UserID:{}, BrokerID:{}, ExchangeID:{}, CombHedgeFlag:{}, ContingentCondition:{}, ForceCloseReason:{}, IsAutoSuspend:{}, IsSwapOrder:{}, MinVolume:{}, TimeCondition:{}, VolumeCondition:{}, StopPrice:{}}",  //
                    logInfo,  //
                    pInputOrder.instrumentID,  //
                    pInputOrder.limitPrice,  //
                    pInputOrder.volumeTotalOriginal,  //
                    pInputOrder.orderPriceType,  //
                    pInputOrder.direction,  //
                    pInputOrder.combOffsetFlag,  //
                    pInputOrder.orderRef,  //
                    pInputOrder.investorID,  //
                    pInputOrder.userID,  //
                    pInputOrder.brokerID,  //
                    pInputOrder.exchangeID,  //
                    pInputOrder.combHedgeFlag,  //
                    pInputOrder.contingentCondition,  //
                    pInputOrder.forceCloseReason,  //
                    pInputOrder.isAutoSuspend,  //
                    pInputOrder.isSwapOrder,  //
                    pInputOrder.minVolume,  //
                    pInputOrder.timeCondition,  //
                    pInputOrder.volumeCondition,  //
                    pInputOrder.stopPrice
                )
            }


        } catch (t: Throwable) {
            logger.error("{}OnErrRtnOrderInsert Exception", logInfo, t)
        }
    }

    override fun OnErrRtnOrderAction(pOrderAction: CThostFtdcOrderActionField?, pRspInfo: CThostFtdcRspInfoField?) {
        if (pRspInfo != null) {
            logger.error(
                "{}????????????????????????(OnErrRtnOrderAction) ??????ID:{},????????????:{}",
                logInfo,
                pRspInfo.errorID,
                pRspInfo.errorMsg
            )
            if (instrumentQueried) {
                val notice = Notice()
                notice.info =
                    logInfo + "??????????????????????????????(OnErrRtnOrderAction) ??????ID:" + pRspInfo.errorID + ",????????????:" + pRspInfo.errorMsg
                notice.infoLevel = InfoLevelEnum.ERROR
                notice.timestamp = System.currentTimeMillis()
                eventService.emit(notice)
            }
        } else {
            logger.error("{}??????????????????????????????(OnErrRtnOrderAction)??????,???????????????", logInfo)
        }
    }

    override fun OnRtnInstrumentStatus(pInstrumentStatus: CThostFtdcInstrumentStatusField?) {

    }

    override fun OnRtnBulletin(pBulletin: CThostFtdcBulletinField?) {

    }

    override fun OnRtnTradingNotice(pTradingNoticeInfo: CThostFtdcTradingNoticeInfoField?) {

    }

    override fun OnRtnErrorConditionalOrder(pErrorConditionalOrder: CThostFtdcErrorConditionalOrderField?) {

    }

    override fun OnRtnExecOrder(pExecOrder: CThostFtdcExecOrderField?) {

    }

    override fun OnErrRtnExecOrderInsert(
        pInputExecOrder: CThostFtdcInputExecOrderField?,
        pRspInfo: CThostFtdcRspInfoField?
    ) {

    }

    override fun OnErrRtnExecOrderAction(
        pExecOrderAction: CThostFtdcExecOrderActionField?,
        pRspInfo: CThostFtdcRspInfoField?
    ) {

    }

    override fun OnErrRtnForQuoteInsert(
        pInputForQuote: CThostFtdcInputForQuoteField?,
        pRspInfo: CThostFtdcRspInfoField?
    ) {

    }

    override fun OnRtnQuote(pQuote: CThostFtdcQuoteField?) {

    }

    override fun OnErrRtnQuoteInsert(pInputQuote: CThostFtdcInputQuoteField?, pRspInfo: CThostFtdcRspInfoField?) {

    }

    override fun OnErrRtnQuoteAction(pQuoteAction: CThostFtdcQuoteActionField?, pRspInfo: CThostFtdcRspInfoField?) {

    }

    override fun OnRtnForQuoteRsp(pForQuoteRsp: CThostFtdcForQuoteRspField?) {

    }

    override fun OnRtnCFMMCTradingAccountToken(pCFMMCTradingAccountToken: CThostFtdcCFMMCTradingAccountTokenField?) {

    }

    override fun OnErrRtnBatchOrderAction(
        pBatchOrderAction: CThostFtdcBatchOrderActionField?,
        pRspInfo: CThostFtdcRspInfoField?
    ) {

    }

    override fun OnRtnOptionSelfClose(pOptionSelfClose: CThostFtdcOptionSelfCloseField?) {

    }

    override fun OnErrRtnOptionSelfCloseInsert(
        pInputOptionSelfClose: CThostFtdcInputOptionSelfCloseField?,
        pRspInfo: CThostFtdcRspInfoField?
    ) {

    }

    override fun OnErrRtnOptionSelfCloseAction(
        pOptionSelfCloseAction: CThostFtdcOptionSelfCloseActionField?,
        pRspInfo: CThostFtdcRspInfoField?
    ) {

    }

    override fun OnRtnCombAction(pCombAction: CThostFtdcCombActionField?) {

    }

    override fun OnErrRtnCombActionInsert(
        pInputCombAction: CThostFtdcInputCombActionField?,
        pRspInfo: CThostFtdcRspInfoField?
    ) {

    }

    override fun OnRspQryContractBank(
        pContractBank: CThostFtdcContractBankField?,
        pRspInfo: CThostFtdcRspInfoField?,
        nRequestID: Int,
        bIsLast: Boolean
    ) {

    }

    override fun OnRspQryParkedOrder(
        pParkedOrder: CThostFtdcParkedOrderField?,
        pRspInfo: CThostFtdcRspInfoField?,
        nRequestID: Int,
        bIsLast: Boolean
    ) {

    }

    override fun OnRspQryParkedOrderAction(
        pParkedOrderAction: CThostFtdcParkedOrderActionField?,
        pRspInfo: CThostFtdcRspInfoField?,
        nRequestID: Int,
        bIsLast: Boolean
    ) {

    }

    override fun OnRspQryTradingNotice(
        pTradingNotice: CThostFtdcTradingNoticeField?,
        pRspInfo: CThostFtdcRspInfoField?,
        nRequestID: Int,
        bIsLast: Boolean
    ) {

    }

    override fun OnRspQryBrokerTradingParams(
        pBrokerTradingParams: CThostFtdcBrokerTradingParamsField?,
        pRspInfo: CThostFtdcRspInfoField?,
        nRequestID: Int,
        bIsLast: Boolean
    ) {

    }

    override fun OnRspQryBrokerTradingAlgos(
        pBrokerTradingAlgos: CThostFtdcBrokerTradingAlgosField?,
        pRspInfo: CThostFtdcRspInfoField?,
        nRequestID: Int,
        bIsLast: Boolean
    ) {

    }

    override fun OnRspQueryCFMMCTradingAccountToken(
        pQueryCFMMCTradingAccountToken: CThostFtdcQueryCFMMCTradingAccountTokenField?,
        pRspInfo: CThostFtdcRspInfoField?,
        nRequestID: Int,
        bIsLast: Boolean
    ) {

    }

    override fun OnRtnFromBankToFutureByBank(pRspTransfer: CThostFtdcRspTransferField?) {

    }

    override fun OnRtnFromFutureToBankByBank(pRspTransfer: CThostFtdcRspTransferField?) {

    }

    override fun OnRtnRepealFromBankToFutureByBank(pRspRepeal: CThostFtdcRspRepealField?) {

    }

    override fun OnRtnRepealFromFutureToBankByBank(pRspRepeal: CThostFtdcRspRepealField?) {

    }

    override fun OnRtnFromBankToFutureByFuture(pRspTransfer: CThostFtdcRspTransferField?) {

    }

    override fun OnRtnFromFutureToBankByFuture(pRspTransfer: CThostFtdcRspTransferField?) {

    }

    override fun OnRtnRepealFromBankToFutureByFutureManual(pRspRepeal: CThostFtdcRspRepealField?) {

    }

    override fun OnRtnRepealFromFutureToBankByFutureManual(pRspRepeal: CThostFtdcRspRepealField?) {

    }

    override fun OnRtnQueryBankBalanceByFuture(pNotifyQueryAccount: CThostFtdcNotifyQueryAccountField?) {

    }

    override fun OnErrRtnBankToFutureByFuture(
        pReqTransfer: CThostFtdcReqTransferField?,
        pRspInfo: CThostFtdcRspInfoField?
    ) {

    }

    override fun OnErrRtnFutureToBankByFuture(
        pReqTransfer: CThostFtdcReqTransferField?,
        pRspInfo: CThostFtdcRspInfoField?
    ) {

    }

    override fun OnErrRtnRepealBankToFutureByFutureManual(
        pReqRepeal: CThostFtdcReqRepealField?,
        pRspInfo: CThostFtdcRspInfoField?
    ) {

    }

    override fun OnErrRtnRepealFutureToBankByFutureManual(
        pReqRepeal: CThostFtdcReqRepealField?,
        pRspInfo: CThostFtdcRspInfoField?
    ) {

    }

    override fun OnErrRtnQueryBankBalanceByFuture(
        pReqQueryAccount: CThostFtdcReqQueryAccountField?,
        pRspInfo: CThostFtdcRspInfoField?
    ) {

    }

    override fun OnRtnRepealFromBankToFutureByFuture(pRspRepeal: CThostFtdcRspRepealField?) {

    }

    override fun OnRtnRepealFromFutureToBankByFuture(pRspRepeal: CThostFtdcRspRepealField?) {

    }

    override fun OnRspFromBankToFutureByFuture(
        pReqTransfer: CThostFtdcReqTransferField?,
        pRspInfo: CThostFtdcRspInfoField?,
        nRequestID: Int,
        bIsLast: Boolean
    ) {

    }

    override fun OnRspFromFutureToBankByFuture(
        pReqTransfer: CThostFtdcReqTransferField?,
        pRspInfo: CThostFtdcRspInfoField?,
        nRequestID: Int,
        bIsLast: Boolean
    ) {

    }

    override fun OnRspQueryBankAccountMoneyByFuture(
        pReqQueryAccount: CThostFtdcReqQueryAccountField?,
        pRspInfo: CThostFtdcRspInfoField?,
        nRequestID: Int,
        bIsLast: Boolean
    ) {

    }

    override fun OnRtnOpenAccountByBank(pOpenAccount: CThostFtdcOpenAccountField?) {

    }

    override fun OnRtnCancelAccountByBank(pCancelAccount: CThostFtdcCancelAccountField?) {

    }

    override fun OnRtnChangeAccountByBank(pChangeAccount: CThostFtdcChangeAccountField?) {

    }
}