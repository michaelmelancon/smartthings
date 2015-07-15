/**
*  Smart Lights
*
*  Copyright 2015 Michael Melancon
*
*  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License. You may obtain a copy of the License at:
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
*  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
*  for the specific language governing permissions and limitations under the License.
*
*/
definition(
    name: "Smart Lights",
    namespace: "melancon",
    author: "Michael Melancon",
    description: "Allows for creating smart light devices that intelligently control the combination of a smart switch and smart bulb(s).",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Categories/lightsAndSwitches.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Categories/lightsAndSwitches@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Categories/lightsAndSwitches@3x.png")


    preferences {
        page(name: "configurationPage")
    }

    def configurationPage() {
        dynamicPage(name: "configurationPage", title: "Configure Smart Lights", install: true, uninstall: true) {
            section("Select the smart switches that control power to smart bulb(s).") {
                input "smartSwitches", "capability.switch", title: "Which smart switch(es)?", multiple: true, required: true, submitOnChange: true
            }
            smartSwitches.each { s ->
                section("Configure a Smart Light for ${s}") {
                    input "smartBulbs${getIndex(s.id)}", "capability.switchLevel", title: "Choose the smart bulb(s)", multiple: true, required: true
                    input "controlStyle${getIndex(s.id)}", "enum", title: "Choose a control style", required: true, defaultValue: "dimmer", options: ["full color", "color temp", "dimmer"]
                }
            }
        }
    }

    def getIndex(switchId) {
        if (state.smartLightInfo == null)
        state.smartLightInfo = [:]
        def info = state.smartLightInfo[(switchId)]
        if (!info) {
            info = [switchId: switchId, index: nextIndex()]
            state.smartLightInfo[(switchId)] = info
        }
        info.index
    }

    def nextIndex() {
        def max = state.smartLightInfo.values().collect{it.index}.max()
        max = (!max) ? 1 : max + 1
        (0..max).find{index -> !state.smartLightInfo.values().any {it.index == index}}
    }

    def installed() {
        log.debug "Installed with settings: ${settings}"
        initialize()
    }

    def updated() {
        log.debug "Updated with settings: ${settings}"

        unsubscribe()
        initialize()
    }

    def initialize() {
        updateSmartLightInfo()
        cleanUpRemovedSmartLights()
    }

    private updateSmartLightInfo() {
    	def oldLightInfo = state.smartLightInfo.clone()
        smartSwitches.each {
            def oldInfo = oldLightInfo[it.id]
            def newInfo = createSmartLightInfo(it.id, oldInfo.index)
            def sl = getChildDevice(newInfo.deviceNetworkId)
            if (oldInfo.type != newInfo.type) {
                if (sl) {
                    deleteChildDevice(newInfo.deviceNetworkId)
                    sl = addSmartLight(newInfo, sl.displayName)
                }
                else
                	sl = addSmartLight(newInfo, "${it} Smart Light")
            }
            state.smartLightInfo[newInfo.switchId] = newInfo
            subscribe(it, 'switch.on', powerOnHandler, [filterEvents: false])
            subscribe(it, 'switch.off', powerOffHandler, [filterEvents: false])
            subscribeToCommand(sl, 'sync', syncHandler)
        }
    }

    private addSmartLight(info, name) {
    	addChildDevice('melancon', info.type, info.deviceNetworkId, null, [completedSetup:true, name:name, displayName:name])
    }

    private cleanUpRemovedSmartLights() {
        def deviceIds = state.smartLightInfo.values().collect { it.deviceNetworkId }
        def removedDevices = getChildDevices().findAll { !deviceIds.contains(it.deviceNetworkId) }
        removedDevices.each {
            deleteChildDevice(it.deviceNetworkId)
        }
    }

    private createSmartLightInfo(String switchId, int index) {
        [switchId: switchId, index: index, deviceNetworkId: getDeviceNetworkId(switchId), type: getSmartLightType(settings."controlStyle${index}")]
    }

    def getSmartLightInfo(smartLight) {
        state.smartLightInfo.values().find { it -> it.deviceNetworkId == smartLight.deviceNetworkId }
    }

    private getDeviceNetworkId(String switchId) {
        "${switchId}/SL".toString()
    }

    private getSmartLightType(controlStyle) {
        String smartLightType = 'Smart Light - DM'
        switch (controlStyle) {
            case 'full color':
            smartLightType = 'Smart Light - FC'
            break
            case 'color temp':
            smartLightType = 'Smart Light - CT'
            break
        }
        smartLightType
    }

    def powerOnHandler(evt) {
        def info = state.smartLightInfo[evt.id]
        if  (info) {
            def sl = getChildDevice(info.deviceNetworkId)
            log.debug "${evt.device} powered on; synchronizing ${sl} to previous state."
            sl.on()
            sl.sync([delay:50])
        }
    }

    def powerOffHandler(evt) {
        def info = state.smartLightInfo[evt.id]
        if  (info) {
            getChildDevice(info.deviceNetworkId).off()
        }
    }

    def syncHandler(evt) {
        sync(evt.device)
    }

    def on(sl) {
        def info = getSmartLightInfo(sl)
        smartSwitches.find{it.id == info.switchId}.on()
        settings."smartBulbs${info.index}".on()
    }

    def off(sl) {
        def info = getSmartLightInfo(sl)
        settings."smartBulbs${info.index}".off()
    }

    def setLevel(sl, percent) {
        log.debug "${sl} Dim level: ${percent}"
        def info = getSmartLightInfo(sl)
        def smartBulbs = settings."smartBulbs${info.index}"
        smartBulbs.setLevel(percent)
    }

    def setHue(sl, percent) {
        log.debug "${sl} Hue: ${percent}"
        def info = getSmartLightInfo(sl)
        def smartBulbs = settings."smartBulbs${info.index}"
        smartBulbs.each {
            if (it.hasCapability("Color Control"))
            it.setHue(percent)
        }
    }

    def setSaturation(sl, percent) {
        log.debug "${sl} Saturation: ${percent}"
        def info = getSmartLightInfo(sl)
        def smartBulbs = settings."smartBulbs${info.index}"
        smartBulbs.each {
            if (it.hasCapability("Color Control"))
            it.setSaturation(percent)
        }
    }

    def setColor(sl, color) {
        color.remove("level")
        def info = getSmartLightInfo(sl)
        def smartBulbs = settings."smartBulbs${info.index}"
        smartBulbs.each {
            if (it.hasCapability("Color Control"))
            it.setColor(color)
        }
    }

    def setColorTemperature(sl, mirek) {
        def info = getSmartLightInfo(sl)
        def smartBulbs = settings."smartBulbs${info.index}"
        smartBulbs.each {
            if (it.hasCommand("setColorTemperature"))
            it.setColorTemperature(mirek)
        }
    }

    def sync(sl) {
        def info = getSmartLightInfo(sl)
        def smartBulbs = settings."smartBulbs${info.index}"
        if (sl.currentSwitch == "on") {
            smartSwitches.find { it.id == info.switchId}.on()
            smartBulbs.on()
            smartBulbs.each {
                if (it.hasCapability("Color Control")) {
                    it.setColor(sl.currentColor, [delay:50])
                }
            }
            smartBulbs.setLevel(sl.currentLevel, [delay:50])
        }
        else {
            smartBulbs.off()
        }
    }
