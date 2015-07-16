/**
*  Smart Light - FC
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
metadata {
	definition (name: "Smart Light - FC", namespace: "melancon", author: "Michael Melancon") {
		capability "Actuator"
		capability "Sensor"
		capability "Switch"
		capability "Switch Level"
		capability "Color Control"

		command "sync"
		command "resetColor"
		command "setColorTemperature", ["number"]

		attribute "colorTemperature", "number"
	}

	standardTile("switch", "device.switch", width:2, height: 2, canChangeIcon: true) {
		state "on", label:'${name}', action:"switch.off", icon:"st.lights.multi-light-bulb-on", backgroundColor:"#79b821", nextState:"turningOff"
		state "off", label:'${name}', action:"switch.on", icon:"st.lights.multi-light-bulb-off", backgroundColor:"#ffffff", nextState:"turningOn"
		state "turningOn", label:'${name}', action:"switch.off", icon:"st.lights.multi-light-bulb-on", backgroundColor:"#79b821", nextState:"turningOff"
		state "turningOff", label:'${name}', action:"switch.on", icon:"st.lights.multi-light-bulb-off", backgroundColor:"#ffffff", nextState:"turningOn"
	}

	standardTile("reset", "device.level", decoration: "flat", inactiveLabel: false) {
		state "default", label:"Reset Color", action:"reset", icon:"st.illuminance.illuminance.dark"
	}

	standardTile("sync", "device.level", decoration: "flat", inactiveLabel: false) {
		state "default", label:"Sync", action:"sync", icon:"st.secondary.refresh-icon"
	}

	controlTile("levelControl", "device.level", "slider", height: 1, width: 3, range:"(0..100)") {
		state "default", action:"switch level.setLevel"
	}

	controlTile("colorTemperatureControl", "device.colorTemperature", "slider", height: 1, width: 3, range:"(153..500)") {
		state "default", action:"setColorTemperature"
	}

	controlTile("colorControl", "device.color", "color", height: 3, width: 3) {
		state "default", action:"setColorWithoutLevel"
	}

	main(["switch"])

	details( ["switch", "sync", "levelControl", "reset", "colorControl", "colorTemperatureControl"])

	preferences()
}

def parse(description) {
}

def on() {
	log.debug "Requesting 'on'"
	sendEvent(name: "switch", value: "on")
	parent.on(this)
}

def off() {
	log.debug "Requesting 'off'"
	sendEvent(name: "switch", value: "off")
	parent.off(this)
}

def setLevel(percent) {
	log.debug "Requesting 'setLevel($percent)'"
	sendEvent(name: "level", value: percent)
	parent.setLevel(this, percent)
}

def setColorTemperature(mirek) {
	log.debug "Requesting 'setColorTemperature($mirek)'"
	sendEvent(name: "colorTemperature", value: mirek)
	parent.setColorTemperature(this, mirek)
}

def setSaturation(percent) {
	log.debug "Requesting 'setSaturation($percent)'"
	sendEvent(name: "saturation", value: percent)
	parent.setSaturation(this, percent)
}

def setHue(percent) {
	log.debug "Requesting 'setHue($percent)'"
	sendEvent(name: "hue", value: percent)
	parent.setHue(this, percent)
}

def setColorWithoutLevel(color) {
	color.remove("level")
    setColor(color)
}

def setColor(color) {
	log.debug "Requesting 'setColor($color)'"
	sendEvent(name: "color", value: color)
    if (color.hue) sendEvent(name: "hue", value: color.hue)
    if (color.saturation) sendEvent(name: "saturation", value: color.saturation)
    if (color.level) sendEvent(name: "level", value: color.level)
    parent.setColor(this, color)
}

def resetColor() {
	log.debug "Executing 'resetColor'"
	setColor([saturation: 0, hue: 0, hex: '#ffffff'])
}

def sync() {
	log.debug "Executing 'sync'"
}
