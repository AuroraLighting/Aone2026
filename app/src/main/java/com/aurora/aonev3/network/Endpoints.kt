package com.aurora.aonev3.network

import com.aurora.aonev3.network.handlers.DevelcoHandler.Endpoints.*
import com.aurora.aonev3.replace
import com.aurora.aonev3.data.devices.Device
import com.aurora.aonev3.data.groups.Group
import com.aurora.aonev3.data.groups.groupmembers.GroupMember
import com.aurora.aonev3.data.groups.scenes.Scene
import com.aurora.aonev3.data.logic.LogicCollection
import com.aurora.aonev3.data.logic.rules.LogicRule
import com.aurora.aonev3.data.logic.timers.LogicTimer

val zigbee = ZIGBEE.url

val devices = DEVICES.url
fun device(device: Device) = device(device.id)
fun device(deviceId: Int) = DEVICE.url.replace(Pair("{ID}", deviceId))

fun deviceLdev(device: Device, ldev: String) = deviceLdev(device.id, ldev)
fun deviceLdev(deviceId: Int, ldev: String) = DEVICE_LDEV.url.replace(Pair("{ID}", deviceId), Pair("{LDEV}", ldev))

fun deviceDatapoint(device: Device, ldev: String, datapoint: String) = deviceDatapoint(device.id, ldev, datapoint)
fun deviceDatapoint(deviceId: Int, ldev: String, datapoint: String) = DEVICE_DATAPOINT.url.replace(Pair("{ID}", deviceId), Pair("{LDEV}", ldev), Pair("{DATAPOINT}", datapoint))

fun deviceData(device: Device, ldev: String) = deviceData(device.id, ldev)
fun deviceData(deviceId: Int, ldev: String) = DEVICE_DATA.url.replace(Pair("{ID}", deviceId), Pair("{LDEV}", ldev))

val groups = GROUPS.url
fun group(group: Group) = group(group.id)
fun group(groupId: Int) = GROUP.url.replace(Pair("{ID}", groupId))

fun groupDatapoint(group: Group, ldev: String = "generic", datapoint: String) = groupDatapoint(group.id, ldev, datapoint)
fun groupDatapoint(groupId: Int, ldev: String = "generic", datapoint: String) = GROUP_DATAPOINT.url.replace(Pair("{ID}", groupId), Pair("{LDEV}", ldev), Pair("{DATAPOINT}", datapoint))

fun groupData(group: Group, ldev: String = "generic") = groupData(group.id, ldev)
fun groupData(groupId: Int, ldev: String = "generic") = GROUP_DATA.url.replace(Pair("{ID}", groupId), Pair("{LDEV}", ldev))

fun groupDatapointAndScenes(group: Group, ldev: String = "generic") = groupDatapointAndScenes(group.id, ldev)
fun groupDatapointAndScenes(groupId: Int, ldev: String = "generic") = GROUP_DATAPOINT_AND_SCENES.url.replace(Pair("{ID}", groupId), Pair("{LDEV}", ldev))

fun groupScenes(group: Group, ldev: String = "generic") = groupScenes(group.id, ldev)
fun groupScenes(groupId: Int, ldev: String = "generic") = GROUP_SCENES.url.replace(Pair("{ID}", groupId), Pair("{LDEV}", ldev))

fun groupScene(group: Group, ldev: String = "generic", scene: Scene) = groupScene(group.id, ldev, scene.id)
fun groupScene(groupId: Int, ldev: String = "generic", scene: Scene) = groupScene(groupId, ldev, scene.id)
fun groupScene(group: Group, ldev: String = "generic", sceneId: Int) = groupScene(group.id, ldev, sceneId)
fun groupScene(groupId: Int, ldev: String = "generic", sceneId: Int) = GROUP_SCENE.url.replace(Pair("{GROUP_ID}", groupId), Pair("{LDEV}", ldev), Pair("{SCENE_ID}", sceneId))

fun groupMembers(group: Group, ldev: String = "generic") = GROUP_MEMBERS.url.replace(Pair("{ID}", group.id), Pair("{LDEV}", ldev))
fun groupMember(group: Group, ldev: String = "generic", member: GroupMember) = GROUP_MEMBER.url.replace(Pair("{GROUP_ID}", group.id), Pair("{LDEV}", ldev), Pair("{MEMBER_ID}", member.id))

val logicCollections = LOGIC_COLLECTIONS.url
fun logicCollection(logicCollection: LogicCollection) = logicCollection(logicCollection.id)
fun logicCollection(logicCollectionId: Int) = LOGIC_COLLECTION.url.replace(Pair("{ID}", logicCollectionId))

fun logicRules(logicCollection: LogicCollection) = logicRules(logicCollection.id)
fun logicRules(logicCollectionId: Int) = LOGIC_RULES.url.replace(Pair("{ID}", logicCollectionId))

fun logicRule(logicCollection: LogicCollection, logicRule: LogicRule) = logicRule(logicCollection.id, logicRule.id)
fun logicRule(logicCollectionId: Int, logicRule: LogicRule) = logicRule(logicCollectionId, logicRule.id)
fun logicRule(logicCollection: LogicCollection, logicRuleId: Int) = logicRule(logicCollection.id, logicRuleId)
fun logicRule(logicCollectionId: Int, logicRuleId: Int) = LOGIC_RULE.url.replace(Pair("{COLLECTION_ID}", logicCollectionId), Pair("{RULE_ID}", logicRuleId))

fun logicTimers(logicCollection: LogicCollection) = logicTimers(logicCollection.id)
fun logicTimers(logicCollectionId: Int) = LOGIC_TIMERS.url.replace(Pair("{ID}", logicCollectionId))

fun logicTimer(logicCollection: LogicCollection, logicTimer: LogicTimer) = logicTimer(logicCollection.id, logicTimer.id)
fun logicTimer(logicCollectionId: Int, logicTimer: LogicTimer) = logicTimer(logicCollectionId, logicTimer.id)
fun logicTimer(logicCollection: LogicCollection, logicTimerId: Int) = logicTimer(logicCollection.id, logicTimerId)
fun logicTimer(logicCollectionId: Int, logicTimerId: Int) = LOGIC_TIMER.url.replace(Pair("{COLLECTION_ID}", logicCollectionId), Pair("{TIMER_ID}", logicTimerId))

fun logicRulesAndTimers(logicCollection: LogicCollection) = logicRulesAndTimers(logicCollection.id)
fun logicRulesAndTimers(logicCollectionId: Int) = LOGIC_RULES_AND_TIMERS.url.replace(Pair("{ID}", logicCollectionId))

val firmwareDownloads = FIRMWARE_DOWNLOADS.url
val firmwareImages = FIRMWARE_IMAGES.url
fun firmwareImage(id: Int) = FIRMWARE_IMAGE.url.replace(Pair("{ID}", id))
val firmwareStatus = FIRMWARE_STATUS.url

val time = TIME.url
val templates = TEMPLATES.url
fun template(hash: String) = TEMPLATE.url.replace(Pair("{HASH}", hash))
