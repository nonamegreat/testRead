package xyz.redtorch.master.sync

import org.slf4j.LoggerFactory
import org.springframework.web.socket.PingMessage
import org.springframework.web.socket.PongMessage
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator
import xyz.redtorch.common.constant.Constant
import xyz.redtorch.common.constant.Constant.KEY_FALSE
import xyz.redtorch.common.constant.Constant.KEY_TRUE
import xyz.redtorch.common.storage.po.GatewaySetting
import xyz.redtorch.common.sync.dto.*
import xyz.redtorch.common.sync.enumeration.ActionEnum
import xyz.redtorch.common.trade.dto.Contract
import xyz.redtorch.common.trade.dto.Order
import xyz.redtorch.common.trade.dto.Tick
import xyz.redtorch.common.trade.dto.Trade
import xyz.redtorch.common.utils.JsonUtils
import xyz.redtorch.common.utils.Lz4Utils
import xyz.redtorch.master.web.socket.SlaveNodeWebSocketHandler
import java.io.IOException
import java.nio.ByteBuffer
import java.util.concurrent.Executors

class SlaveNodeTransceiver(
    private val slaveNodeWebSocketHandler: SlaveNodeWebSocketHandler,
    session: WebSocketSession
) : ConcurrentWebSocketSessionDecorator(session, 30 * 1000, Integer.MAX_VALUE) {

    companion object {
        private val logger = LoggerFactory.getLogger(SlaveNodeTransceiver::class.java)
    }

    var slaveNodeId: String? = null
        private set

    var isAuthed = false
        private set

    val establishedTimestamp = System.currentTimeMillis()

    private var pingStartTimestamp = 0L
    private var delay = 0L

    var slaveNodeSettingMirror: SlaveNodeSettingMirror = SlaveNodeSettingMirror()
        private set

    var portfolioMirror: PortfolioMirror = PortfolioMirror()
        private set

    var baseMirror: BaseMirror = BaseMirror()
        private set

    var transactionMirror: TransactionMirror = TransactionMirror()
        private set

    var quoteMirror: QuoteMirror = QuoteMirror()
        private set

    var slaveNodeReportMirror: SlaveNodeReportMirror = SlaveNodeReportMirror()
        private set

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
                                "??????Action,sessionId={},slaveNodeId={},ActionEnum={},Data={}",
                                id,
                                slaveNodeId,
                                action.actionEnum,
                                JsonUtils.mapper.writeValueAsString(JsonUtils.readToJsonNode(action.data!!))
                            )
                        } else {
                            logger.info("??????Action,sessionId={},slaveNodeId={},??????ActionEnum={}", id, slaveNodeId, action.actionEnum)
                        }
                    }

                    // ??????????????????
                    when (action.actionEnum) {
                        ActionEnum.TransactionMirrorPatch -> {
                            handleTransactionMirrorPatch(action)
                        }
                        ActionEnum.QuoteMirrorPatch -> {
                            handleQuoteMirrorPatch(action)
                        }
                        ActionEnum.TradeRtnPatch -> {
                            handleTradeRtnPatch(action)
                        }
                        ActionEnum.OrderRtnPatch -> {
                            handleOrderRtnPatch(action)
                        }
                        ActionEnum.PortfolioMirrorPatch -> {
                            handlePortfolioMirrorPatch(action)
                        }
                        ActionEnum.BaseMirrorPatch -> {
                            handleBaseMirrorPatch(action)
                        }
                        ActionEnum.SlaveNodeStatusMirrorPatch -> {
                            handleSlaveNodeReportMirrorPatch(action)
                        }
                        ActionEnum.TickRtnPatch -> {
                            handleTickRtnPatch(action)
                        }
                        ActionEnum.NoticeRtn -> {
                            handleNoticeRtn(action)
                        }
                        else -> logger.error("???????????????Action,sessionId={},slaveNodeId={},Action={}", id, slaveNodeId, payload)
                    }

                }
            } catch (e: Exception) {
                logger.error("??????????????????,sessionId={},slaveNodeId={}", id, slaveNodeId, e)
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
        logger.info("??????Pong,sessionId={},slaveNodeId={},??????{}ms", id, slaveNodeId, delay)
    }

    override fun close() {
        logger.info("????????????,sessionId={},slaveNodeId={}", id, slaveNodeId)
        isAuthed = false
        slaveNodeWebSocketHandler.sessionIdToTransceiverMapRemove(id)
        slaveNodeId?.let {
            slaveNodeWebSocketHandler.slaveNodeIdToTransceiverMapRemove(it)
        }


        try {
            if (!singleThreadExecutor.isShutdown) {
                singleThreadExecutor.shutdownNow()
            }
        } catch (e: Exception) {
            logger.error("?????????????????????,sessionId={},slaveNodeId={}", id, slaveNodeId, e)
        }

        if (isOpen) {
            try {
                super.close()
            } catch (e: Exception) {
                logger.warn("????????????????????????,sessionId={},slaveNodeId={}", id, slaveNodeId, e)
            }
        }
    }

    private fun sendTextMessage(data: String) {
        if (isOpen) {
            if (data.length > 100 * 1024) {
                val compressedData = Lz4Utils.frameCompress(data)
                sendMessage(TextMessage(Constant.LZ4FRAME_HEADER + compressedData))
            } else {
                sendMessage(TextMessage(data))
            }
        }
    }

    private fun handleAuth(action: Action) {
        // ??????????????????
        val auth = JsonUtils.readToObject(action.data!!, Auth::class.java)

        slaveNodeId = auth.id!!

        // ?????????????????????
        val slaveNode = slaveNodeWebSocketHandler.slaveNodeService.slaveNodeAuth(auth.id!!, auth.token!!)
        if (slaveNode != null) {
            slaveNodeWebSocketHandler.slaveNodeIdToTransceiverMapPut(slaveNodeId!!, this)
            // ????????????
            isAuthed = true
            // ??????????????????????????????
            sendTextMessage(JsonUtils.writeToJsonString(Action().apply {
                actionEnum = ActionEnum.AuthResult
                data = KEY_TRUE
            }))
        } else {
            // ??????????????????????????????
            sendTextMessage(JsonUtils.writeToJsonString(Action().apply {
                actionEnum = ActionEnum.AuthResult
                data = KEY_FALSE
            }))
            logger.warn("??????????????????,????????????,sessionId={},authId={}", id, auth.id)
            close()
        }
    }

    private fun handleTransactionMirrorPatch(action: Action) {
        transactionMirror = JsonUtils.applyJsonPatch(transactionMirror, action.data!!)
    }

    private fun handleTradeRtnPatch(action: Action) {
        val tradeRtnPatch = JsonUtils.readToObject(action.data!!, TradeRtnPatch::class.java)

        val trade = if (tradeRtnPatch.uniformSymbol.isNullOrBlank()) {
            tradeRtnPatch.trade!!
        } else {
            val oldTrade = Trade()
            baseMirror.contractMap[tradeRtnPatch.uniformSymbol]?.let {
                oldTrade.contract = it
            }
            JsonUtils.applyJsonPatch(oldTrade, tradeRtnPatch.jsonPath!!)
        }

        transactionMirror.tradeMap[trade.tradeId] = trade

        slaveNodeWebSocketHandler.process(trade)

    }

    private fun handleOrderRtnPatch(action: Action) {
        val orderRtnPatch = JsonUtils.readToObject(action.data!!, OrderRtnPatch::class.java)

        val order = if (orderRtnPatch.orderId.isNullOrBlank()) {
            orderRtnPatch.order!!
        } else {
            val orderId = orderRtnPatch.orderId!!

            val oldOrder = if (transactionMirror.orderMap.containsKey(orderId)) {
                transactionMirror.orderMap[orderId]!!
            } else {
                Order()
            }
            JsonUtils.applyJsonPatch(oldOrder, orderRtnPatch.jsonPath!!)
        }

        transactionMirror.orderMap[order.orderId] = order

        slaveNodeWebSocketHandler.process(order)
    }

    private fun handleQuoteMirrorPatch(action: Action) {
        quoteMirror = JsonUtils.applyJsonPatch(quoteMirror, action.data!!)
    }

    private fun handleTickRtnPatch(action: Action) {
        val tickRtnPatch = JsonUtils.readToObject(action.data!!, TickRtnPatch::class.java)

        val tick = if (tickRtnPatch.uniformSymbol.isNullOrBlank()) {
            tickRtnPatch.tick!!
        } else {
            val uniformSymbol = tickRtnPatch.uniformSymbol!!

            val oldTick = if (quoteMirror.tickMap.containsKey(uniformSymbol)) {
                quoteMirror.tickMap[uniformSymbol]!!
            } else {
                Tick()
            }
            JsonUtils.applyJsonPatch(oldTick, tickRtnPatch.jsonPath!!)
        }

        quoteMirror.tickMap[tick.contract.uniformSymbol] = tick

        slaveNodeWebSocketHandler.process(tick)
    }

    private fun handlePortfolioMirrorPatch(action: Action) {
        portfolioMirror = JsonUtils.applyJsonPatch(portfolioMirror, action.data!!)
    }

    private fun handleBaseMirrorPatch(action: Action) {
        baseMirror = JsonUtils.applyJsonPatch(baseMirror, action.data!!)
    }

    private fun handleSlaveNodeReportMirrorPatch(action: Action) {
        slaveNodeReportMirror = JsonUtils.applyJsonPatch(slaveNodeReportMirror, action.data!!)
    }

    private fun handleNoticeRtn(action: Action) {
        val notice = JsonUtils.readToObject(action.data!!, Notice::class.java)
        slaveNodeWebSocketHandler.process(notice)
    }

    /**
     * ??????????????????
     */
    @Synchronized
    fun updateSlaveNodeSettingMirror(subscribedList: List<Contract>, gatewaySettingList: List<GatewaySetting>) {
        val targetSlaveNodeSettingMirror = SlaveNodeSettingMirror()

        for (contract in subscribedList) {
            targetSlaveNodeSettingMirror.subscribedMap[contract.uniformSymbol] = contract
        }
        for (gatewaySetting in gatewaySettingList) {
            targetSlaveNodeSettingMirror.gatewaySettingMap[gatewaySetting.id!!] = gatewaySetting
        }

        val jsonPatch = JsonUtils.diffAsJsonPatch(slaveNodeSettingMirror, targetSlaveNodeSettingMirror)

        if (!jsonPatch.isEmpty) {
            val action = Action().apply {
                actionEnum = ActionEnum.SlaveNodeSettingMirrorPatch
                data = jsonPatch.toString()
            }
            sendTextMessage(JsonUtils.writeToJsonString(action))
            slaveNodeSettingMirror = targetSlaveNodeSettingMirror
        }
    }

    fun cancelOrder(cancelOrder: CancelOrder) {
        val action = Action().apply {
            actionEnum = ActionEnum.CancelOrder
            data = JsonUtils.writeToJsonString(cancelOrder)
        }
        sendTextMessage(JsonUtils.writeToJsonString(action))
    }

    fun submitOrder(insertOrder: InsertOrder) {
        val action = Action().apply {
            actionEnum = ActionEnum.InsertOrder
            data = JsonUtils.writeToJsonString(insertOrder)
        }
        sendTextMessage(JsonUtils.writeToJsonString(action))
    }

}