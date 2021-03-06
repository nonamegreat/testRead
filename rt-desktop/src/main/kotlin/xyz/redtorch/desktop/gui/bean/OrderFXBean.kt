package xyz.redtorch.desktop.gui.bean

import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.VBox
import javafx.scene.text.Text
import xyz.redtorch.common.constant.Constant
import xyz.redtorch.common.trade.dto.Order
import xyz.redtorch.common.trade.enumeration.*
import xyz.redtorch.common.utils.CommonUtils

class OrderFXBean(order: Order, contractSelectedFlag: Boolean) {

    private val orderId = SimpleStringProperty()
    private val contract = SimpleObjectProperty<Pane>()
    private val direction = SimpleObjectProperty<Text>()
    private val offsetFlag = SimpleStringProperty()
    private val hedgeFlag = SimpleStringProperty()
    private val orderPriceType = SimpleStringProperty()
    private val price = SimpleStringProperty()
    private val volume = SimpleObjectProperty<Pane>()
    private val orderStatus = SimpleObjectProperty<Text>()
    private val statusMsg = SimpleStringProperty()
    private val orderTime = SimpleStringProperty()
    private val timeCondition = SimpleStringProperty()
    private val volumeCondition = SimpleStringProperty()
    private val minVolume = SimpleIntegerProperty()
    private val contingentCondition = SimpleStringProperty()
    private val stopPrice = SimpleStringProperty()
    private val adapterOrderId = SimpleStringProperty()
    private val originOrderId = SimpleStringProperty()
    private val accountId = SimpleStringProperty()

    private var decimalDigits = 4

    private var contractSelectedFlag = false

    var orderDTO: Order? = null
        private set

    init {
        update(order, contractSelectedFlag)
    }

    fun update(newOrderDTO: Order, newContractSelectedFlag: Boolean) {


        if (orderDTO == null) {
            decimalDigits = CommonUtils.getNumberDecimalDigits(newOrderDTO.contract.priceTick)
            updateOrderId(newOrderDTO)
            updateDirection(newOrderDTO)
            updateOffsetFlag(newOrderDTO)
            updateHedgeFlag(newOrderDTO)
            updateOrderPriceType(newOrderDTO)
            updateContract(newOrderDTO)
            updatePrice(newOrderDTO)
            updateOrderTime(newOrderDTO)
            updateTimeCondition(newOrderDTO)
            updateVolumeCondition(newOrderDTO)
            updateMinVolume(newOrderDTO)
            updateContingentCondition(newOrderDTO)
            updateStopPrice(newOrderDTO)
            updateAdapterOrderId(newOrderDTO)
            updateAccountId(newOrderDTO)
        }

        if (contractSelectedFlag != newContractSelectedFlag) {
            contractSelectedFlag = newContractSelectedFlag
            updateContract(newOrderDTO)
        }
        // ??????????????????
        if (newOrderDTO !== orderDTO) {
            updateChangeable(newOrderDTO)
            orderDTO = newOrderDTO
        }
    }

    private fun updateChangeable(newOrderDTO: Order) {
        updateOrderStatus(newOrderDTO)
        updateStatusMsg(newOrderDTO)
        updateVolume(newOrderDTO)
        updateOriginOrderId(newOrderDTO)
    }

    private fun updateOrderId(newOrderDTO: Order) {
        setOrderId(newOrderDTO.orderId)
    }

    private fun updateContract(newOrderDTO: Order) {
        val vBox = VBox().apply {
            children.add(Text(newOrderDTO.contract.uniformSymbol).apply {
                // ??????????????????
                if (contractSelectedFlag) {
                    styleClass.add("trade-remind-color")
                }
            })
            children.add(
                Text(newOrderDTO.contract.name)
            )
            userData = newOrderDTO
        }

        setContract(vBox)
    }

    private fun updateDirection(newOrderDTO: Order) {
        val directionText = Text()
        when (newOrderDTO.direction) {
            DirectionEnum.Buy -> {
                directionText.text = "???"
                directionText.styleClass.add("trade-long-color")
            }
            DirectionEnum.Sell -> {
                directionText.text = "???"
                directionText.styleClass.add("trade-short-color")
            }
            DirectionEnum.Unknown -> {
                directionText.text = "??????"
            }
            else -> {
                directionText.text = newOrderDTO.direction.toString()
            }
        }

        directionText.userData = newOrderDTO
        setDirection(directionText)
    }

    private fun updateOffsetFlag(newOrderDTO: Order) {
        val offsetFlag = when (newOrderDTO.offsetFlag) {
            OffsetFlagEnum.Close -> {
                "???"
            }
            OffsetFlagEnum.CloseToday -> {
                "??????"
            }
            OffsetFlagEnum.CloseYesterday -> {
                "??????"
            }
            OffsetFlagEnum.Open -> {
                "???"
            }
            OffsetFlagEnum.Unknown -> {
                "??????"
            }
            else -> {
                newOrderDTO.offsetFlag.toString()
            }
        }

        setOffsetFlag(offsetFlag)
    }

    private fun updateHedgeFlag(newOrderDTO: Order) {

        val hedgeFlag = when (newOrderDTO.hedgeFlag) {
            HedgeFlagEnum.Speculation -> {
                "??????"
            }
            HedgeFlagEnum.Hedge -> {
                "??????"
            }
            HedgeFlagEnum.Arbitrage -> {
                "??????"
            }
            HedgeFlagEnum.MarketMaker -> {
                "?????????"
            }
            HedgeFlagEnum.SpecHedge -> {
                "???????????????????????????????????? ???????????????"
            }
            HedgeFlagEnum.HedgeSpec -> {
                "???????????????????????????????????? ???????????????"
            }
            HedgeFlagEnum.Unknown -> {
                "??????"
            }
            else -> {
                newOrderDTO.hedgeFlag.toString()
            }
        }
        setHedgeFlag(hedgeFlag)
    }

    private fun updateOrderPriceType(newOrderDTO: Order) {
        val orderPriceType = when (newOrderDTO.orderPriceType) {
            OrderPriceTypeEnum.LimitPrice -> {
                "??????"
            }
            OrderPriceTypeEnum.AnyPrice -> {
                "??????"
            }
            OrderPriceTypeEnum.BestPrice -> {
                "?????????"
            }
            OrderPriceTypeEnum.LastPrice -> {
                "?????????"
            }
            OrderPriceTypeEnum.LastPricePlusOneTicks -> {
                "?????????????????????1???ticks"
            }
            OrderPriceTypeEnum.LastPricePlusThreeTicks -> {
                "?????????????????????3???ticks"
            }
            OrderPriceTypeEnum.Unknown -> {
                "??????"
            }
            else -> {
                newOrderDTO.orderPriceType.toString()
            }
        }
        setOrderPriceType(orderPriceType)
    }

    private fun updatePrice(newOrderDTO: Order) {
        setPrice(CommonUtils.formatDouble(newOrderDTO.price, decimalDigits))
    }

    private fun updateVolume(newOrderDTO: Order) {
        if (orderDTO == null || orderDTO!!.orderStatus !== newOrderDTO.orderStatus || orderDTO!!.tradedVolume != newOrderDTO.tradedVolume) {
            val vBox = VBox().apply {
                children.add(HBox().apply {
                    children.add(Text("??????").apply {
                        wrappingWidth = 35.0
                        styleClass.add("trade-label")
                    })
                    children.add(Text(newOrderDTO.totalVolume.toString()).apply {
                        // ???????????????????????????,??????
                        if (Constant.ORDER_STATUS_WORKING_SET.contains(newOrderDTO.orderStatus)) {
                            if (newOrderDTO.direction == DirectionEnum.Buy) {
                                styleClass.add("trade-long-color")
                            } else if (newOrderDTO.direction == DirectionEnum.Sell) {
                                styleClass.add("trade-short-color")
                            }
                        }
                    })
                })

                children.add(HBox().apply {
                    children.add(Text("??????").apply {
                        wrappingWidth = 35.0
                        styleClass.add("trade-label")
                    })
                    children.add(Text(newOrderDTO.tradedVolume.toString()))
                })

                userData = newOrderDTO
            }

            setVolume(vBox)
        }
    }

    private fun updateOrderStatus(newOrderDTO: Order) {
        if (orderDTO == null || orderDTO!!.orderStatus !== newOrderDTO.orderStatus) {
            val orderStatusText = Text()
            when (newOrderDTO.orderStatus) {
                OrderStatusEnum.AllTraded -> {
                    orderStatusText.text = "????????????"
                }
                OrderStatusEnum.Canceled -> {
                    orderStatusText.text = "?????????"
                }
                OrderStatusEnum.NoTradeQueueing -> {
                    orderStatusText.text = "????????????????????????"
                    orderStatusText.styleClass.add("trade-remind-color")
                }
                OrderStatusEnum.NoTradeNotQueueing -> {
                    orderStatusText.text = "????????????????????????"
                    orderStatusText.styleClass.add("trade-remind-color")
                }
                OrderStatusEnum.PartTradedQueueing -> {
                    orderStatusText.text = "???????????????????????????"
                    orderStatusText.styleClass.add("trade-remind-color")
                }
                OrderStatusEnum.PartTradedNotQueueing -> {
                    orderStatusText.text = "???????????????????????????"
                    orderStatusText.styleClass.add("trade-remind-color")
                }
                OrderStatusEnum.Rejected -> {
                    orderStatusText.text = "??????"
                }
                OrderStatusEnum.NotTouched -> {
                    orderStatusText.text = "?????????"
                    orderStatusText.styleClass.add("trade-remind-color")
                }
                OrderStatusEnum.Touched -> {
                    orderStatusText.text = "?????????"
                }
                OrderStatusEnum.Unknown -> {
                    orderStatusText.text = "??????"
                }
                else -> {
                    orderStatusText.text = newOrderDTO.orderStatus.toString()
                }
            }
            orderStatusText.userData = newOrderDTO
            setOrderStatus(orderStatusText)
        }
    }

    private fun updateStatusMsg(newOrderDTO: Order) {
        if (orderDTO == null || orderDTO!!.statusMsg != newOrderDTO.statusMsg) {
            setStatusMsg(newOrderDTO.statusMsg)
        }
    }

    private fun updateOrderTime(newOrderDTO: Order) {
        if (orderDTO == null || orderDTO!!.orderTime != newOrderDTO.orderTime) {
            setOrderTime(newOrderDTO.orderTime)
        }
    }

    private fun updateTimeCondition(newOrderDTO: Order) {
        val timeCondition = when (newOrderDTO.timeCondition) {
            TimeConditionEnum.GFA -> {
                "(GFA)??????????????????"
            }
            TimeConditionEnum.GFD -> {
                "(GFD)????????????"
            }
            TimeConditionEnum.GFS -> {
                "(GFS)????????????"
            }
            TimeConditionEnum.GTC -> {
                "(GTC)???????????????"
            }
            TimeConditionEnum.GTD -> {
                "(GTD)?????????????????????"
            }
            TimeConditionEnum.IOC -> {
                "(IOC)????????????,????????????"
            }
            TimeConditionEnum.Unknown -> {
                "??????"
            }
            else -> {
                newOrderDTO.timeCondition.toString()
            }
        }
        setTimeCondition(timeCondition)
    }

    private fun updateVolumeCondition(newOrderDTO: Order) {
        val volumeCondition = when (newOrderDTO.volumeCondition) {
            VolumeConditionEnum.AV -> {
                "????????????"
            }
            VolumeConditionEnum.CV -> {
                "????????????"
            }
            VolumeConditionEnum.MV -> {
                "????????????"
            }
            VolumeConditionEnum.Unknown -> {
                "??????"
            }
            else -> {
                newOrderDTO.volumeCondition.toString()
            }
        }
        setVolumeCondition(volumeCondition)
    }

    private fun updateMinVolume(newOrderDTO: Order) {
        setMinVolume(newOrderDTO.minVolume)
    }

    private fun updateContingentCondition(newOrderDTO: Order) {
        val contingentCondition = when (newOrderDTO.contingentCondition) {
            ContingentConditionEnum.Immediately -> {
                "??????"
            }
            ContingentConditionEnum.LocalLastPriceGreaterEqualStopPrice -> {
                "(??????)??????????????????????????????"
            }
            ContingentConditionEnum.LocalLastPriceLesserEqualStopPrice -> {
                "(??????)??????????????????????????????"
            }
            ContingentConditionEnum.LastPriceGreaterEqualStopPrice -> {
                "??????????????????????????????"
            }
            ContingentConditionEnum.LastPriceLesserEqualStopPrice -> {
                "??????????????????????????????"
            }
            ContingentConditionEnum.Unknown -> {
                "??????"
            }
            else -> {
                newOrderDTO.contingentCondition.toString()
            }
        }
        setContingentCondition(contingentCondition)
    }

    private fun updateStopPrice(newOrderDTO: Order) {
        setStopPrice(CommonUtils.formatDouble(newOrderDTO.stopPrice, decimalDigits))
    }

    private fun updateOriginOrderId(newOrderDTO: Order) {
        if (orderDTO == null || orderDTO!!.originOrderId != newOrderDTO.originOrderId) {
            setOriginOrderId(newOrderDTO.originOrderId)
        }
    }

    private fun updateAdapterOrderId(newOrderDTO: Order) {
        setAdapterOrderId(newOrderDTO.adapterOrderId)
    }

    private fun updateAccountId(newOrderDTO: Order) {
        setAccountId(newOrderDTO.accountId)
    }

    fun getOrderId(): String {
        return orderId.get()
    }

    fun orderIdProperty(): SimpleStringProperty {
        return orderId
    }

    fun setOrderId(orderId: String) {
        this.orderId.set(orderId)
    }

    fun getContract(): Pane {
        return contract.get()
    }

    fun contractProperty(): SimpleObjectProperty<Pane> {
        return contract
    }

    fun setContract(contract: Pane) {
        this.contract.set(contract)
    }

    fun getDirection(): Text {
        return direction.get()
    }

    fun directionProperty(): SimpleObjectProperty<Text> {
        return direction
    }

    fun setDirection(direction: Text) {
        this.direction.set(direction)
    }

    fun getOffsetFlag(): String {
        return offsetFlag.get()
    }

    fun offsetFlagProperty(): SimpleStringProperty {
        return offsetFlag
    }

    fun setOffsetFlag(
        offsetFlag: String
    ) {
        this.offsetFlag.set(offsetFlag)
    }

    fun getHedgeFlag(): String {
        return hedgeFlag.get()
    }

    fun hedgeFlagProperty(): SimpleStringProperty {
        return hedgeFlag
    }

    fun setHedgeFlag(
        hedgeFlag: String
    ) {
        this.hedgeFlag.set(hedgeFlag)
    }

    fun getOrderPriceType(): String {
        return orderPriceType.get()
    }

    fun orderPriceTypeProperty(): SimpleStringProperty {
        return orderPriceType
    }

    fun setOrderPriceType(
        orderPriceType: String
    ) {
        this.orderPriceType.set(orderPriceType)
    }

    fun getPrice(): String {
        return price.get()
    }

    fun priceProperty(): SimpleStringProperty {
        return price
    }

    fun setPrice(
        price: String
    ) {
        this.price.set(price)
    }

    fun getVolume(): Pane {
        return volume.get()
    }

    fun volumeProperty(): SimpleObjectProperty<Pane> {
        return volume
    }

    fun setVolume(volume: Pane) {
        this.volume.set(volume)
    }

    fun getOrderStatus(): Text {
        return orderStatus.get()
    }

    fun orderStatusProperty(): SimpleObjectProperty<Text> {
        return orderStatus
    }

    fun setOrderStatus(orderStatus: Text) {
        this.orderStatus.set(orderStatus)
    }

    fun getStatusMsg(): String {
        return statusMsg.get()
    }

    fun statusMsgProperty(): SimpleStringProperty {
        return statusMsg
    }

    fun setStatusMsg(
        statusMsg: String
    ) {
        this.statusMsg.set(statusMsg)
    }

    fun getOrderTime(): String {
        return orderTime.get()
    }

    fun orderTimeProperty(): SimpleStringProperty {
        return orderTime
    }

    fun setOrderTime(
        orderTime: String
    ) {
        this.orderTime.set(orderTime)
    }

    fun getTimeCondition(): String {
        return timeCondition.get()
    }

    fun timeConditionProperty(): SimpleStringProperty {
        return timeCondition
    }

    fun setTimeCondition(timeCondition: String) {
        this.timeCondition.set(timeCondition)
    }

    fun getVolumeCondition(): String {
        return volumeCondition.get()
    }

    fun volumeConditionProperty(): SimpleStringProperty {
        return volumeCondition
    }

    fun setVolumeCondition(
        volumeCondition: String
    ) {
        this.volumeCondition.set(volumeCondition)
    }

    fun getMinVolume(): Int {
        return minVolume.get()
    }

    fun minVolumeProperty(): SimpleIntegerProperty {
        return minVolume
    }

    fun setMinVolume(minVolume: Int) {
        this.minVolume.set(minVolume)
    }

    fun getContingentCondition(): String {
        return contingentCondition.get()
    }

    fun contingentConditionProperty(): SimpleStringProperty {
        return contingentCondition
    }

    fun setContingentCondition(
        contingentCondition: String
    ) {
        this.contingentCondition.set(contingentCondition)
    }

    fun getStopPrice(): String {
        return stopPrice.get()
    }

    fun stopPriceProperty(): SimpleStringProperty {
        return stopPrice
    }

    fun setStopPrice(
        stopPrice: String
    ) {
        this.stopPrice.set(stopPrice)
    }

    fun getAdapterOrderId(): String {
        return adapterOrderId.get()
    }

    fun adapterOrderIdProperty(): SimpleStringProperty {
        return adapterOrderId
    }

    fun setAdapterOrderId(
        adapterOrderId: String
    ) {
        this.adapterOrderId.set(adapterOrderId)
    }

    fun getOriginOrderId(): String {
        return originOrderId.get()
    }

    fun originOrderIdProperty(): SimpleStringProperty {
        return originOrderId
    }

    fun setOriginOrderId(
        originOrderId: String
    ) {
        this.originOrderId.set(originOrderId)
    }

    fun getAccountId(): String {
        return accountId.get()
    }

    fun accountIdProperty(): SimpleStringProperty {
        return accountId
    }

    fun setAccountId(
        accountId: String
    ) {
        this.accountId.set(accountId)
    }

}