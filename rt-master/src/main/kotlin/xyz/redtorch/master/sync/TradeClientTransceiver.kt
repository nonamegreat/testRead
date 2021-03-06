package xyz.redtorch.master.sync

import com.fasterxml.jackson.core.type.TypeReference
import org.slf4j.LoggerFactory
import org.springframework.web.socket.PingMessage
import org.springframework.web.socket.PongMessage
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator
import xyz.redtorch.common.constant.Constant
import xyz.redtorch.common.storage.po.User
import xyz.redtorch.common.sync.dto.*
import xyz.redtorch.common.sync.enumeration.ActionEnum
import xyz.redtorch.common.sync.enumeration.InfoLevelEnum
import xyz.redtorch.common.trade.dto.*
import xyz.redtorch.common.utils.JsonUtils
import xyz.redtorch.common.utils.Lz4Utils
import xyz.redtorch.master.web.socket.TradeClientWebSocketHandler
import java.io.IOException
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import java.util.concurrent.locks.ReentrantLock

class TradeClientTransceiver(private val tradeClientWebSocketHandler: TradeClientWebSocketHandler, session: WebSocketSession) :
    ConcurrentWebSocketSessionDecorator(session, 30 * 1000, Integer.MAX_VALUE) {

    companion object {
        private val logger = LoggerFactory.getLogger(TradeClientTransceiver::class.java)
    }

    var userId: String? = null
        private set

    var user: User? = null
        private set

    var isAuthed = false
        private set

    val establishedTimestamp = System.currentTimeMillis()

    private var pingStartTimestamp = 0L
    private var delay = 0L

    private var baseMirror = BaseMirror()
    private val baseMirrorLock = ReentrantLock()
    private var portfolioMirror = PortfolioMirror()
    private var transactionMirror = TransactionMirror()
    private val transactionMirrorLock = ReentrantLock()
    private var quoteMirror = QuoteMirror()
    private val quoteMirrorLock = ReentrantLock()

    private val singleThreadExecutor = Executors.newSingleThreadExecutor()

    fun handleTextMessage(message: TextMessage) {
        singleThreadExecutor.execute {
            try {
                var payload = message.payload

                // ?????????LZ4Frame????????????????????????
                if (payload.startsWith(Constant.LZ4FRAME_HEADER)) {
                    payload = Lz4Utils.frameDecompress(payload.slice(Constant.LZ4FRAME_HEADER.length until payload.length))!!
                }

                // ????????????
                val action = JsonUtils.mapper.readValue(payload, Action::class.java)

                // ????????????????????????????????????
                if (!isAuthed) {
                    // ???????????????.????????????????????????
                    if (action.actionEnum == ActionEnum.Auth) {
                        handleAuth(action)
                    } else {
                        // ????????????????????????,???????????????
                        logger.warn("???????????????,???????????????Action,??????,sessionId={}", id)
                        close()
                    }
                } else {
                    if (Constant.isDebugEnable) {
                        if (action.data != null
                            && action.actionEnum != ActionEnum.BaseMirrorPatch // ??????????????????,?????????Action??????
                        ) {
                            logger.info(
                                "??????Action,sessionId={},userId={},ActionEnum={},Data={}",
                                id,
                                userId,
                                action.actionEnum,
                                JsonUtils.mapper.writeValueAsString(JsonUtils.readToJsonNode(action.data!!))
                            )
                        } else {
                            logger.info("??????Action,sessionId={},userId={},??????ActionEnum={}", id, userId, action.actionEnum)
                        }
                    }

                    // ??????????????????
                    when (action.actionEnum) {
                        ActionEnum.InsertOrder -> {
                            handleInsertOrder(action)
                        }
                        ActionEnum.CancelOrder -> {
                            handleCancelOrder(action)
                        }
                        ActionEnum.Subscribe -> {
                            handleSubscribe(action)
                        }
                        ActionEnum.Unsubscribe -> {
                            handleUnsubscribe(action)
                        }
                        else -> logger.error("???????????????Action,sessionId={},userId={},Action={}", id, userId, payload)
                    }

                }
            } catch (e: Exception) {
                logger.error("??????????????????,sessionId={},userId={}", id, userId, e)
                close()
            }
        }
    }

    fun ping(pingStartTimestamp: Long) {
        if (isAuthed) {
            if (this.pingStartTimestamp != 0L) {
                if (pingStartTimestamp - this.pingStartTimestamp > 20 * 1000) {
                    logger.error("Ping??????,sessionId={}", id)
                    close()
                }
            } else {
                this.pingStartTimestamp = pingStartTimestamp
                try {
                    val byteBuffer = ByteBuffer.allocate(java.lang.Long.BYTES).putLong(this.pingStartTimestamp).flip()
                    val message = PingMessage(byteBuffer)
                    sendMessage(message)
                } catch (e: IOException) {
                    logger.error("??????Ping??????,sessionId={}", id, e)
                }
            }
        } else {
            logger.error("??????Ping??????,???????????????,sessionId={}", id)
        }
    }

    fun getDelay(): Long {
        return delay
    }

    fun handlePoneMessage(message: PongMessage) {
        this.pingStartTimestamp = 0L
        val pingStartTimestamp = message.payload.asLongBuffer().get()
        this.delay = System.currentTimeMillis() - pingStartTimestamp
        logger.info("??????Pong,sessionId={},userId={},??????{}ms", id, userId, delay)
    }

    override fun close() {
        logger.info("????????????,sessionId={},userId={}", id, userId)
        isAuthed = false
        tradeClientWebSocketHandler.sessionIdToTransceiverMapRemove(id)
        userId?.let {
            tradeClientWebSocketHandler.userIdToTransceiverMapRemove(it)
        }

        try {
            if (!singleThreadExecutor.isShutdown) {
                singleThreadExecutor.shutdownNow()
            }
        } catch (e: Exception) {
            logger.error("?????????????????????????????????,sessionId={},userId={}", id, userId, e)
        }

        if (isOpen) {
            try {
                super.close()
            } catch (e: Exception) {
                logger.warn("????????????????????????,sessionId={},userId={}", id, userId, e)
            }
        }
    }

    private fun sendTextMessage(data: String) {
        if (data.length > 100 * 1024) {
            val compressedData = Lz4Utils.frameCompress(data)
            sendMessage(TextMessage(Constant.LZ4FRAME_HEADER + compressedData))
        } else {
            sendMessage(TextMessage(data))
        }
    }

    private fun handleAuth(action: Action) {
        // ??????????????????
        val auth = JsonUtils.readToObject(action.data!!, Auth::class.java)

        userId = auth.id!!

        // ?????????????????????
        val user = tradeClientWebSocketHandler.userService.userAuth(auth.id!!, auth.token!!)
        if (user != null && !user.banned) {
            // ????????????
            tradeClientWebSocketHandler.userIdToTransceiverMapPut(userId!!, this)
            this.user = user
            isAuthed = true
            // ??????????????????????????????
            sendTextMessage(JsonUtils.writeToJsonString(Action().apply {
                actionEnum = ActionEnum.AuthResult
                data = Constant.KEY_TRUE
            }))

        } else {
            if (user != null && user.banned) {
                logger.warn("??????????????????,?????????,sessionId={},authId={}", id, auth.id)
            }

            // ??????????????????????????????
            sendTextMessage(JsonUtils.writeToJsonString(Action().apply {
                actionEnum = ActionEnum.AuthResult
                data = Constant.KEY_FALSE
            }))
            logger.warn("??????????????????,????????????,sessionId={},authId={}", id, auth.id)
            close()
        }
    }

    private fun handleInsertOrder(action: Action) {
        // ??????????????????
        tradeClientWebSocketHandler.submitOrderExecutor.execute {
            try {
                // ?????????????????????????????????
                val validUser = user!!

                val insertOrder = JsonUtils.readToObject(action.data!!, InsertOrder::class.java)

                val accountId = "${insertOrder.accountCode}@${insertOrder.currency}@${insertOrder.gatewayId}"
                val notice = Notice().apply {
                    infoLevel = InfoLevelEnum.ERROR
                    info = "??????????????????,???????????????,accountId=$accountId"
                }

                if (!validUser.permitTradeAllAccounts) {
                    if (!validUser.acceptTradeAccountIdSet.contains(accountId)) {
                        logger.warn("??????????????????,???????????????,sessionId={},userId={},accountId={}", id, userId, accountId)
                        sendNotice(notice)
                        return@execute
                    }
                }
                if (validUser.denyTradeAccountIdSet.contains(accountId)) {
                    logger.warn("??????????????????,???????????????,sessionId={},userId={},accountId={}", id, userId, accountId)
                    sendNotice(notice)
                    return@execute
                }

                val uniformSymbol: String
                if (insertOrder.contract == null) {
                    notice.info = "??????????????????,??????????????????"
                    logger.warn("??????????????????,??????????????????,sessionId={},userId={}", id, userId)
                    sendNotice(notice)
                    return@execute
                } else {
                    uniformSymbol = insertOrder.contract!!.uniformSymbol
                }

                if (!validUser.permitTradeAllContracts) {
                    if (!validUser.acceptTradeUniformSymbolSet.contains(uniformSymbol)) {
                        notice.info = "??????????????????,???????????????,uniformSymbol=${uniformSymbol}"
                        logger.warn("??????????????????,???????????????,sessionId={},userId={},uniformSymbol={}", id, userId, uniformSymbol)
                        sendNotice(notice)
                        return@execute
                    }
                }
                if (validUser.denyTradeUniformSymbolSet.contains(uniformSymbol)) {
                    notice.info = "??????????????????,???????????????,uniformSymbol=${uniformSymbol}"
                    logger.warn("??????????????????,???????????????,sessionId={},userId={},uniformSymbol={}", id, userId, uniformSymbol)
                    sendNotice(notice)
                    return@execute
                }

                tradeClientWebSocketHandler.systemService.submitOrder(insertOrder)
            } catch (e: Exception) {
                logger.warn("????????????????????????,Action={}", JsonUtils.writeToJsonString(action), e)
            }
        }

    }

    private fun handleUnsubscribe(action: Action) {
        val contractList = JsonUtils.mapper.readValue(action.data!!, object : TypeReference<List<Contract>>() {})
        quoteMirrorLock.lock()
        try {

            val quoteMirror = this.quoteMirror.clone()

            for (contract in contractList) {
                quoteMirror.subscribedMap.remove(contract.uniformSymbol)
                quoteMirror.tickMap.remove(contract.uniformSymbol)
            }

            val quoteMirrorJsonPatch = JsonUtils.diffAsJsonPatch(this.quoteMirror, quoteMirror)

            if (!quoteMirrorJsonPatch.isEmpty) {
                this.quoteMirror = quoteMirror

                // ??????
                tradeClientWebSocketHandler.updateSubscribedMap()

                val returnAction = Action().apply {
                    actionEnum = ActionEnum.QuoteMirrorPatch
                    data = quoteMirrorJsonPatch.toString()
                }
                sendTextMessage(JsonUtils.writeToJsonString(returnAction))
            }

        } finally {
            quoteMirrorLock.unlock()
        }
    }

    private fun handleSubscribe(action: Action) {

        val contractList = JsonUtils.mapper.readValue(action.data!!, object : TypeReference<List<Contract>>() {})
        quoteMirrorLock.lock()
        try {

            val quoteMirror = this.quoteMirror.clone()

            for (contract in contractList) {
                quoteMirror.subscribedMap[contract.uniformSymbol] = contract
            }

            val quoteMirrorJsonPatch = JsonUtils.diffAsJsonPatch(this.quoteMirror, quoteMirror)

            if (!quoteMirrorJsonPatch.isEmpty) {
                this.quoteMirror = quoteMirror

                // ??????
                tradeClientWebSocketHandler.updateSubscribedMap()

                val returnAction = Action().apply {
                    actionEnum = ActionEnum.QuoteMirrorPatch
                    data = quoteMirrorJsonPatch.toString()
                }
                sendTextMessage(JsonUtils.writeToJsonString(returnAction))
            }

        } finally {
            quoteMirrorLock.unlock()
        }

    }

    private fun handleCancelOrder(action: Action) {
        // ????????????????????????
        val cancelOrder = JsonUtils.readToObject(action.data!!, CancelOrder::class.java)
        tradeClientWebSocketHandler.systemService.cancelOrder(cancelOrder)
    }

    fun sendOrder(order: Order) {

        if (!isAuthed) {
            return
        }

        val validUser = user!!
        if ((validUser.permitReadAllAccounts || validUser.acceptReadAccountIdSet.contains(order.accountId)) && !validUser.denyReadAccountIdSet.contains(order.accountId)) {

            transactionMirrorLock.lock()
            try {
                val orderRtnPatch = OrderRtnPatch()
                if (transactionMirror.orderMap.containsKey(order.orderId)) {
                    // ??????Patch
                    val oldOrder = transactionMirror.orderMap[order.orderId]!!
                    orderRtnPatch.orderId = order.orderId
                    orderRtnPatch.jsonPath = JsonUtils.diffAsJsonPatch(oldOrder, order)
                } else {
                    // ???????????????Order
                    orderRtnPatch.orderId = ""
                    orderRtnPatch.order = order
                }
                // ????????????Order
                transactionMirror.orderMap[order.orderId] = order

                val action = Action().apply {
                    actionEnum = ActionEnum.OrderRtnPatch
                    data = JsonUtils.writeToJsonString(orderRtnPatch)
                }
                sendTextMessage(JsonUtils.writeToJsonString(action))
            } finally {
                transactionMirrorLock.unlock()
            }

        }
    }

    fun sendTrade(trade: Trade) {

        if (!isAuthed) {
            return
        }

        val validUser = user!!
        if ((validUser.permitReadAllAccounts || validUser.acceptReadAccountIdSet.contains(trade.accountId)) && !validUser.denyReadAccountIdSet.contains(trade.accountId)) {

            baseMirrorLock.lock()
            try {
                val tradeRtnPatch = TradeRtnPatch()
                if (baseMirror.contractMap.containsKey(trade.contract.uniformSymbol)) {
                    // ??????????????????,?????????Patch
                    val sourceTrade = Trade()
                    sourceTrade.contract = baseMirror.contractMap[trade.contract.uniformSymbol]!!

                    tradeRtnPatch.uniformSymbol = trade.contract.uniformSymbol
                    tradeRtnPatch.jsonPath = JsonUtils.diffAsJsonPatch(sourceTrade, trade)
                } else {
                    // ????????????????????????
                    tradeRtnPatch.uniformSymbol = ""
                    tradeRtnPatch.trade = trade
                }

                val action = Action().apply {
                    actionEnum = ActionEnum.TradeRtnPatch
                    data = JsonUtils.writeToJsonString(tradeRtnPatch)
                }
                sendTextMessage(JsonUtils.writeToJsonString(action))
            } finally {
                baseMirrorLock.unlock()
            }

            // ??????
            transactionMirrorLock.lock()
            try {
                transactionMirror.tradeMap[trade.tradeId] = trade
            } finally {
                transactionMirrorLock.unlock()
            }
        }
    }


    fun sendNotice(notice: Notice) {
        if(!isAuthed){
            return
        }

        val action = Action().apply {
            actionEnum = ActionEnum.NoticeRtn
            data = JsonUtils.writeToJsonString(notice)
        }
        sendTextMessage(JsonUtils.writeToJsonString(action))
    }

    fun sendTick(tick: Tick) {

        if (!isAuthed) {
            return
        }
        quoteMirrorLock.lock()
        try {
            if (quoteMirror.subscribedMap.contains(tick.contract.uniformSymbol)) {

                val tickRtnPatch = TickRtnPatch()
                if (quoteMirror.tickMap.containsKey(tick.contract.uniformSymbol)) {
                    // ??????Patch
                    val oldTick = quoteMirror.tickMap[tick.contract.uniformSymbol]!!
                    tickRtnPatch.uniformSymbol = tick.contract.uniformSymbol
                    tickRtnPatch.jsonPath = JsonUtils.diffAsJsonPatch(oldTick, tick)
                } else {
                    // ???????????????Tick
                    tickRtnPatch.uniformSymbol = ""
                    tickRtnPatch.tick = tick
                }
                // ????????????Tick
                quoteMirror.tickMap[tick.contract.uniformSymbol] = tick


                val action = Action().apply {
                    actionEnum = ActionEnum.TickRtnPatch
                    data = JsonUtils.writeToJsonString(tickRtnPatch)
                }

                sendTextMessage(JsonUtils.writeToJsonString(action))
            }
        } finally {
            quoteMirrorLock.unlock()
        }
    }

    fun updatePortfolioMirror(accountList: List<Account>, positionList: List<Position>) {

        if (!isAuthed) {
            return
        }

        val portfolioMirror = PortfolioMirror()
        val validUser = user!!
        for (account in accountList) {
            if ((validUser.permitReadAllAccounts || validUser.acceptReadAccountIdSet.contains(account.accountId)) && !validUser.denyReadAccountIdSet.contains(
                    account.accountId
                )
            ) {
                portfolioMirror.accountMap[account.accountId] = account
            }
        }
        for (position in positionList) {
            if ((validUser.permitReadAllAccounts || validUser.acceptReadAccountIdSet.contains(position.accountId)) && !validUser.denyReadAccountIdSet.contains(
                    position.accountId
                )
            ) {
                portfolioMirror.positionMap[position.positionId] = position
            }
        }

        val portfolioMirrorJsonPath = JsonUtils.diffAsJsonPatch(this.portfolioMirror, portfolioMirror)

        if (!portfolioMirrorJsonPath.isEmpty) {
            this.portfolioMirror = portfolioMirror

            val action = Action().apply {
                actionEnum = ActionEnum.PortfolioMirrorPatch
                data = portfolioMirrorJsonPath.toString()
            }

            sendTextMessage(JsonUtils.writeToJsonString(action))
        }

    }

    fun updateTransactionMirror(orderList: List<Order>, tradeList: List<Trade>) {
        if (!isAuthed) {
            return
        }

        val transactionMirror = TransactionMirror()
        val validUser = user!!
        for (order in orderList) {
            if ((validUser.permitReadAllAccounts || validUser.acceptReadAccountIdSet.contains(order.accountId)) && !validUser.denyReadAccountIdSet.contains(
                    order.accountId
                )
            ) {
                transactionMirror.orderMap[order.orderId] = order
            }
        }
        for (trade in tradeList) {
            if ((validUser.permitReadAllAccounts || validUser.acceptReadAccountIdSet.contains(trade.accountId)) && !validUser.denyReadAccountIdSet.contains(
                    trade.accountId
                )
            ) {
                transactionMirror.tradeMap[trade.tradeId] = trade
            }
        }

        transactionMirrorLock.lock()
        try {
            val transactionMirrorJsonPath = JsonUtils.diffAsJsonPatch(this.transactionMirror, transactionMirror)

            if (!transactionMirrorJsonPath.isEmpty) {
                this.transactionMirror = transactionMirror

                val action = Action().apply {
                    actionEnum = ActionEnum.TransactionMirrorPatch
                    data = transactionMirrorJsonPath.toString()
                }

                sendTextMessage(JsonUtils.writeToJsonString(action))
            }
        } finally {
            transactionMirrorLock.unlock()
        }


    }

    fun updateQuoteMirror(tickList: List<Tick>) {
        if (!isAuthed) {
            return
        }

        val quoteMirror = QuoteMirror()

        quoteMirrorLock.lock()
        try {
            quoteMirror.subscribedMap.putAll(this.quoteMirror.subscribedMap)

            for (tick in tickList) {
                if (quoteMirror.subscribedMap.contains(tick.contract.uniformSymbol)) {
                    quoteMirror.tickMap[tick.contract.uniformSymbol] = tick
                }
            }

            val quoteMirrorJsonPatch = JsonUtils.diffAsJsonPatch(this.quoteMirror, quoteMirror)

            if (!quoteMirrorJsonPatch.isEmpty) {
                this.quoteMirror = quoteMirror

                val action = Action().apply {
                    actionEnum = ActionEnum.QuoteMirrorPatch
                    data = quoteMirrorJsonPatch.toString()
                }
                sendTextMessage(JsonUtils.writeToJsonString(action))
            }
        } finally {
            quoteMirrorLock.unlock()
        }

    }

    fun updateBaseMirror(contractList: List<Contract>) {
        if (!isAuthed) {
            return
        }

        val baseMirror = BaseMirror()
        for (contract in contractList) {
            baseMirror.contractMap[contract.uniformSymbol] = contract
        }
        val baseMirrorJsonPatch = JsonUtils.diffAsJsonPatch(this.baseMirror, baseMirror)

        if (!baseMirrorJsonPatch.isEmpty) {
            this.baseMirror = baseMirror

            val action = Action().apply {
                actionEnum = ActionEnum.BaseMirrorPatch
                data = baseMirrorJsonPatch.toString()
            }
            sendTextMessage(JsonUtils.writeToJsonString(action))
        }
    }

    fun getSubscribedMap(): Map<String, Contract> {
        val subscribedMap = HashMap<String, Contract>()
        quoteMirrorLock.lock()
        try {
            subscribedMap.putAll(quoteMirror.subscribedMap)
        } finally {
            quoteMirrorLock.unlock()
        }
        return subscribedMap
    }

}