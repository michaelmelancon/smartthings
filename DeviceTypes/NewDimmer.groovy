metadata {
	definition (name: "Virtual Dimmer", author: "michaelmelancon@gmail.com") {
        capability "Actuator"
        capability "Sensor"
		capability "Switch"
        capability "Switch Level"
		capability "Refresh"
		capability "Polling"

		command "levelUp"
		command "levelDown"
	}

    tiles {

        standardTile("switch", "device.switch", width: 2, height: 2, canChangeIcon: true) {
			state "on", label: '${name}', action: "switch.off", icon: "st.switches.light.on", backgroundColor: "#79b821"
			state "off", label: '${name}', action: "switch.on", icon: "st.switches.light.off", backgroundColor: "#ffffff"
        }

        controlTile("levelSliderControl", "device.level", "slider", height: 1, width: 2, inactiveLabel: false, range: (0..100)) {
            state "level", action:"switch level.setLevel", backgroundColor:"#ffe71e"
        }

        valueTile("level", "device.level", inactiveLabel: false, decoration: "flat") {
			state "level", label: 'Level ${currentValue}%'
		}

        standardTile("brighter", "device.switch", inactiveLabel: false, decoration: "flat", canChangeIcon: false) {
                        state "up", label:'', action:"levelUp", icon:"st.illuminance.illuminance.bright"
        }

        standardTile("dimmer", "device.switch", inactiveLabel: false, decoration: "flat", canChangeIcon: false) {
                        state "down", label:'', action:"levelDown", icon:"st.illuminance.illuminance.light"
        }

        main(["switch"])

        details(["switch", "brighter",  "dimmer",  "levelSliderControl", "level"])
    }

    preferences {
		input "stepSize", "number", title: "Dimming Increment (1-25):", range: (1..25)
	}
}

def installed() {
    initializeState()
	log.debug "installed"
}

def updated() {
    initializeState()
    log.debug "updated"
}

def initializeState()
{
    if (stepSize) {
        state.stepSize = stepSize
    }
    if (state.stepSize == null) {
        state.stepSize = 10
        log.debug "The default dimming increment of ${state.stepSize} was set."
    }
    if (device.latestValue("level") == null) {
        setLevel(100)
        log.debug "The default dimmer level of 100 was set."
    }
}

def parse(String description) {
	log.debug "message: ${description}"
}

def on() {
    log.debug "on"
    sendEvent(name:"switch", value:"on")
    setLevel(device.latestValue("level") as int)
}

def off() {
    log.debug "off"
    sendEvent(name:"switch", value:"off")
}

def setLevel(value, duration) {
    int level = Math.max(Math.min(value as int, 100), 0)
	int dimmingDuration = duration < 128 ? duration : 128 + Math.round(duration / 60)

	def switchState = device.latestValue("switch")

    if (level > 0 && switchState == "off") {
	    on()
    }
    sendEvent(name:"level", value:level)
    sendEvent(name:"switch.setLevel", value:level, descriptionText:"setLevel(${level})")
    log.debug "setLevel(${level})"
}

def setLevel(value) {
	setLevel(value, 0)
}

def levelUp() {
    setLevel((device.latestValue("level") as int) + (state.stepSize as int))
}

def levelDown(){
    setLevel((device.latestValue("level") as int) - (state.stepSize as int))
}

def poll() {
    log.debug "poll"
}

def refresh() {
    log.debug "refresh"
}
