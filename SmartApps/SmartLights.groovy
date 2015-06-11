/**
 *  Light Group
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
    description: "Allows for creating a smart light device that intelligently controls the combination of a smart switch and smart bulb(s).",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Categories/lightsAndSwitches.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Categories/lightsAndSwitches@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Categories/lightsAndSwitches@2x.png")


preferences {
	section("Select the smart switch that controls power to the smart bulb(s).") {
		input "smartSwitch", "capability.switch", label: "Which smart switch?", multiple: false, required: true
	}

	section("Select the smart bulb(s) connected to the selected smart switch.") {
		input "smartBulbs", "capability.switchLevel", label: "Which smart bulbs?", multiple: true, required: true
	}

    section("Select the lighting control style.") {
    	input "controlStyle", "enum", options:["full color", "color temp", "dimmer"]
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
    def master = getChildDevice("SL001")
    if (!master)
    	master = addChildDevice("melancon", "Smart Light", "SL001", null, [label: name, completedSetup: true])
    subscribe(master, "switch.on", onHandler)
	subscribe(master, "switch.off", offHandler)
	subscribe(master, "level", dimHandler)
    subscribe(master, "hue", hueHandler)
    subscribe(master, "saturation", saturationHandler)
    subscribe(master, "color", colorHandler)
    subscribeToCommand(master, "sync", syncHandler)
    subscribe(smartSwitch, "switch.on", powerOnHandler)
    if (smartSwitch.currentSwitch == "on") {
    	master.on()
        master.setLevel(100);
    }
    else {
        master.setLevel(100);
    	master.off()
    }
}

def getControlStyle() {
	controlStyle
}

def onHandler(evt) {
	log.debug evt.value
    smartSwitch.on()
    smartBulbs.each {
    	if (it.hasCapability("Switch"))
        	it.on()
    }
}

def powerOnHandler(evt) {
	log.debug "power on - about to sync"
    def master = getChildDevice("SL001")
    master.on()
    sync(master)
}

def sync(child) {
    if (child.currentSwitch == "on") {
    	smartSwitch.on()
        smartBulbs.on()
        smartBulbs.each {
        	if (it.hasCapability("Color Control")) {
                it.setHue(child.currentHue, [delay:50])
                it.setSaturation(child.currentSaturation, [delay:50])
            }
        }
       	smartBulbs.setLevel(child.currentLevel, [delay:50])
    }
    else {
    	smartBulbs.off()
    }
}

def offHandler(evt) {
	log.debug evt.value
    smartBulbs.each {
    	if (it.hasCapability("Switch"))
        	it.off()
    }
}

def dimHandler(evt) {
	log.debug "Dim level: ${evt.value}"
	smartBulbs.each {
    	if (it.hasCapability("Switch Level"))
        	it.setLevel(evt.numericValue)
    }
}

def colorHandler(evt) {
	log.debug "Color: ${evt.value}"
    smartBulbs.each {
    	if (it.hasCapability("Color Control"))
        	it.setColor(evt.value)
    }
}

def hueHandler(evt) {
	log.debug "Hue: ${evt.value}"
	smartBulbs.each {
    	if (it.hasCapability("Color Control"))
        	it.setHue(evt.numericValue)
    }
}

def saturationHandler(evt) {
	log.debug "Saturation: ${evt.value}"
	smartBulbs.each {
    	if (it.hasCapability("Color Control"))
        	it.setSaturation(evt.numericValue)
    }
}

def syncHandler(evt) {
	log.debug "Syncing"
    def master = getChildDevice("SL001")
	sync(master)
}
