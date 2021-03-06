package xyz.redtorch.master.service.impl

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import xyz.redtorch.common.enumeration.ConnectionStatusEnum
import xyz.redtorch.common.storage.po.GatewaySetting
import xyz.redtorch.common.storage.po.SlaveNode
import xyz.redtorch.master.dao.GatewaySettingDao
import xyz.redtorch.master.service.GatewaySettingService
import xyz.redtorch.master.service.SlaveNodeService
import xyz.redtorch.master.web.socket.SlaveNodeWebSocketHandler

@Service
class GatewaySettingServiceImpl : GatewaySettingService {

    private val logger: Logger = LoggerFactory.getLogger(GatewaySettingServiceImpl::class.java)

    @Autowired
    lateinit var gatewaySettingDao: GatewaySettingDao

    @Autowired
    lateinit var slaveNodeService: SlaveNodeService

    @Autowired
    private lateinit var slaveNodeWebSocketHandler: SlaveNodeWebSocketHandler

    override fun getGatewaySettingById(gatewayId: String): GatewaySetting? {
        return gatewaySettingDao.queryById(gatewayId)
    }

    override fun getGatewaySettingList(): List<GatewaySetting> {
        val slaveNodeList = slaveNodeService.getSlaveNodeList()
        val slaveNodeMap = HashMap<String, SlaveNode>()
        for (slaveNode in slaveNodeList) {
            slaveNodeMap[slaveNode.id!!] = slaveNode
        }
        val gatewaySettingList = gatewaySettingDao.queryList()
        for (gatewaySetting in gatewaySettingList) {
            if (gatewaySetting.connectionStatus == ConnectionStatusEnum.Connecting) {
                if (slaveNodeMap.containsKey(gatewaySetting.targetSlaveNodeId) && slaveNodeMap[gatewaySetting.targetSlaveNodeId]!!.connectionStatus == ConnectionStatusEnum.Connected) {
                    val transceiver = slaveNodeWebSocketHandler.slaveNodeIdToTransceiverMapGet(gatewaySetting.targetSlaveNodeId)
                    if (transceiver != null) {
                        val gatewayStatus = transceiver.slaveNodeReportMirror.gatewayStatusMap[gatewaySetting.id]
                        if (gatewayStatus != null && gatewayStatus.connectionStatus == ConnectionStatusEnum.Connected) {
                            gatewaySetting.connectionStatus = ConnectionStatusEnum.Connected
                        }
                    }
                }
            } else if (gatewaySetting.connectionStatus == ConnectionStatusEnum.Disconnecting) {
                if (slaveNodeMap.containsKey(gatewaySetting.targetSlaveNodeId) && slaveNodeMap[gatewaySetting.targetSlaveNodeId]!!.connectionStatus == ConnectionStatusEnum.Connected) {
                    val transceiver = slaveNodeWebSocketHandler.slaveNodeIdToTransceiverMapGet(gatewaySetting.targetSlaveNodeId)
                    if (transceiver != null) {
                        val gatewayStatus = transceiver.slaveNodeReportMirror.gatewayStatusMap[gatewaySetting.id]
                        if (gatewayStatus == null || gatewayStatus.connectionStatus == ConnectionStatusEnum.Disconnected) {
                            gatewaySetting.connectionStatus = ConnectionStatusEnum.Disconnected
                        }
                    }
                }
            }
        }
        return gatewaySettingList
    }

    override fun upsertGatewaySettingById(gatewaySetting: GatewaySetting) {
        if (gatewaySetting.description == null) {
            gatewaySetting.description = ""
        }
        if (gatewaySetting.name == null) {
            gatewaySetting.name = ""
        }
        if (gatewaySetting.implementClassName == null) {
            gatewaySetting.implementClassName = ""
        }

        if (gatewaySetting.id.isNullOrBlank()) {
            val gatewaySettingList: List<GatewaySetting> = gatewaySettingDao.queryList()
            if (gatewaySettingList.isEmpty()) {
                // ????????????????????????GatewaySetting,??????1001?????????????????????GatewaySetting
                gatewaySetting.id = "1001"
            } else {
                // ?????????????????????GatewaySetting,????????????GatewaySettingID?????????
                val intGatewaySettingIdSet: MutableSet<Int> = HashSet()
                var maxIntGatewaySettingId = 1001
                for (tempGatewaySetting in gatewaySettingList) {
                    val tempIntGatewaySettingId = Integer.valueOf(tempGatewaySetting.id)
                    intGatewaySettingIdSet.add(tempIntGatewaySettingId)
                    if (tempIntGatewaySettingId > maxIntGatewaySettingId) {
                        maxIntGatewaySettingId = tempIntGatewaySettingId
                    }
                }

                // ???????????????+1???????????????
                var newIntGatewaySettingId = maxIntGatewaySettingId + 1

                // ??????????????????????????????
                // ????????????????????????????????????????????????
                // ???????????????????????????[0,1000]?????????ID?????????
                while (intGatewaySettingIdSet.contains(newIntGatewaySettingId) || newIntGatewaySettingId in 0..1000) {
                    newIntGatewaySettingId++
                }
                gatewaySetting.id = newIntGatewaySettingId.toString()
            }
        }


        // ??????????????????
        if (gatewaySetting.connectionStatus == ConnectionStatusEnum.Connected) {
            gatewaySetting.connectionStatus = ConnectionStatusEnum.Connecting
        } else if (gatewaySetting.connectionStatus == ConnectionStatusEnum.Disconnected) {
            gatewaySetting.connectionStatus = ConnectionStatusEnum.Disconnecting
        }

        gatewaySettingDao.upsert(gatewaySetting)

    }

    override fun deleteGatewayById(gatewayId: String) {
        gatewaySettingDao.deleteById(gatewayId)
    }

    override fun connectGatewayById(gatewayId: String) {
        val gatewaySetting: GatewaySetting? = getGatewaySettingById(gatewayId)
        if (gatewaySetting == null) {
            logger.warn("????????????Id????????????,?????????????????????,gatewayId={}", gatewayId)
            return
        }
        if (gatewaySetting.connectionStatus == ConnectionStatusEnum.Disconnected //
            || gatewaySetting.connectionStatus == ConnectionStatusEnum.Disconnecting //
            || gatewaySetting.connectionStatus == ConnectionStatusEnum.Unknown //
        ) {
            gatewaySetting.connectionStatus = ConnectionStatusEnum.Connecting
            upsertGatewaySettingById(gatewaySetting)
        } else {
            logger.info("????????????Id????????????,????????????????????????,gatewayId={},connectionStatus={}", gatewayId, gatewaySetting.connectionStatus)
        }
    }

    override fun disconnectGatewayById(gatewayId: String) {
        val gatewaySetting: GatewaySetting? = getGatewaySettingById(gatewayId)
        if (gatewaySetting == null) {
            logger.warn("????????????Id????????????,?????????????????????,Id={}", gatewayId)
            return
        }
        if (gatewaySetting.connectionStatus == ConnectionStatusEnum.Connected //
            || gatewaySetting.connectionStatus == ConnectionStatusEnum.Connecting //
            || gatewaySetting.connectionStatus == ConnectionStatusEnum.Unknown //
        ) {
            gatewaySetting.connectionStatus = ConnectionStatusEnum.Disconnecting
            upsertGatewaySettingById(gatewaySetting)
        } else {
            logger.info("????????????Id????????????,????????????????????????,gatewayId={},connectionStatus={}", gatewayId, gatewaySetting.connectionStatus)
        }
    }

    override fun connectAllGateways() {
        val gatewaySettingList: List<GatewaySetting> = gatewaySettingDao.queryList()
        for (gatewaySetting in gatewaySettingList) {
            if (gatewaySetting.connectionStatus == ConnectionStatusEnum.Disconnecting //
                || gatewaySetting.connectionStatus == ConnectionStatusEnum.Disconnected //
                || gatewaySetting.connectionStatus == ConnectionStatusEnum.Unknown //
            ) {
                gatewaySetting.connectionStatus = ConnectionStatusEnum.Connecting
                upsertGatewaySettingById(gatewaySetting)
            } else {
                logger.info("??????????????????,????????????????????????,gatewayId={},connectionStatus={}", gatewaySetting.id, gatewaySetting.connectionStatus)
            }
        }
    }

    override fun disconnectAllGateways() {
        val gatewaySettingList: List<GatewaySetting> = gatewaySettingDao.queryList()
        for (gatewaySetting in gatewaySettingList) {
            if (gatewaySetting.connectionStatus == ConnectionStatusEnum.Connecting //
                || gatewaySetting.connectionStatus == ConnectionStatusEnum.Connected //
                || gatewaySetting.connectionStatus == ConnectionStatusEnum.Unknown //
            ) {
                gatewaySetting.connectionStatus = ConnectionStatusEnum.Disconnecting
                upsertGatewaySettingById(gatewaySetting)
            } else {
                logger.info("??????????????????,????????????????????????,gatewayId={},connectionStatus={}", gatewaySetting.id, gatewaySetting.connectionStatus)
            }
        }
    }

    override fun updateGatewaySettingDescriptionById(gatewayId: String, description: String) {
        val gatewaySetting = gatewaySettingDao.queryById(gatewayId)
        if (gatewaySetting != null) {
            gatewaySetting.description = description
            gatewaySettingDao.upsert(gatewaySetting)
        } else {
            logger.warn("????????????????????????,???????????????,gatewayId={}", gatewayId)
        }
    }

    override fun deleteGatewaySettingById(gatewayId: String) {
        gatewaySettingDao.deleteById(gatewayId)
    }

}