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
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Categories/lightsAndSwitches@2x.png")


preferences {
	page(name: "configurationPage")
}

def configurationPage() {
	dynamicPage(name: "configurationPage", title: "Configure Smart Lights", install: true, uninstall: true) {
        section("Select the smart switches that control power to smart bulb(s).") {
            input "smartSwitches", "capability.switch", title: "Which smart switch(es)?", multiple: true, required: true, submitOnChange: true
        }
        smartSwitches.eachWithIndex { s, i ->
			section("Configure a Smart Light for ${s}") {
	        	input "smartLightName${i}", "text", title: "Name this smart light", required: true, defaultValue: "${s} Smart Light"
            	input "icon${i}", "icon", title: "Choose an icon", required: false
				input "smartBulbs${i}", "capability.switchLevel", title: "Choose the smart bulb(s)", multiple: true, required: true
                input "controlStyle${i}", "enum", title: "Choose a control style", required: true, defaultValue: "dimmer", options: ["full color", "color temp", "dimmer"]
            }
        }
    }
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
    smartSwitches.each {
    	def info = state.smartLightInfo[it.id]
        log.debug info
      	def sl = getChildDevice(info.deviceNetworkId)
        if (!sl)
            sl = addChildDevice('melancon', info.type, info.deviceNetworkId, null, [completedSetup:true, icon:info.icon])
        sl.name = info.name
      	sl.displayName = info.name
        subscribe(it, 'switch.on', powerOnHandler, [filterEvents: false])
        subscribe(it, 'switch.off', powerOffHandler, [filterEvents: false])
        subscribeToCommand(sl, 'sync', syncHandler)
    }
}

private cleanUpRemovedSmartLights() {
    def deviceIds = state.smartLightInfo.values().collect { it.deviceNetworkId }
    def removedDevices = getChildDevices().findAll { !deviceIds.contains(it.deviceNetworkId) }
    removedDevices.each {
        unsubscribe(it)
        deleteChildDevice(it)
    }
}

private updateSmartLightInfo()
{
	if (!state.smartLightInfo)
    	state.smartLightInfo = [:]
    def lightInfo = [:]
    smartSwitches.eachWithIndex { it, i ->
    	def oldInfo = state.smartLightInfo[it.id]
    	def newInfo = createSmartLightInfo(it.id, i)
        if (!oldInfo || oldInfo.type != newInfo.type) {
        	def sl = getChildDevice(newInfo.deviceNetworkId)
            if (sl) {
                unsubscribe(sl)
                deleteChildDevice(newInfo.deviceNetworkId)
            }
        }
        lightInfo[newInfo.switchId] = newInfo
    }
    state.smartLightInfo = lightInfo
}

private createSmartLightInfo(switchId, index) {
	[switchId: switchId, index: index, deviceNetworkId: getDeviceNetworkId(switchId), name: settings."smartLightName${index}", type: getSmartLightType(settings."controlStyle${index}"), icon: settings."icon${index}"]
}

def getSmartLightInfo(smartLight) {
	state.smartLightInfo.values().find { it -> it.deviceNetworkId == smartLight.deviceNetworkId }
}

private getDeviceNetworkId(switchId) {
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
    smartSwitches.get(info.index).on()
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
        smartSwitches.get(info.index).on()
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
