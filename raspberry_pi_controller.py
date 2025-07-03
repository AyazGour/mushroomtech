#!/usr/bin/env python3
"""
Raspberry Pi Environmental Control System
- Controls 4-channel relay module for AC appliances
- Monitors temperature and humidity with DHT22
- Receives commands from ESP32 water level monitor
- Provides API for mobile application
- Automatic temperature and humidity control
"""

import RPi.GPIO as GPIO
import Adafruit_DHT
import time
import json
import threading
import logging
from datetime import datetime
from flask import Flask, request, jsonify
import sqlite3
import requests
from dataclasses import dataclass
from typing import Dict, Any, Optional

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('environmental_control.log'),
        logging.StreamHandler()
    ]
)
logger = logging.getLogger(__name__)

# GPIO Pin Configuration
RELAY_PINS = {
    'HUMIDIFIER': 18,      # Relay 1 - 240V AC Humidifier
    'WATER_PUMP': 19,      # Relay 2 - 1HP Water Pump (240V AC)
    'AQUARIUM_PUMP': 20,   # Relay 3 - Aquarium pump (controlled by ESP32)
    'HEATER': 21           # Relay 4 - 240V AC Heater
}

DHT22_PIN = 4  # DHT22 sensor pin

# Temperature and Humidity Thresholds
TEMP_MIN = 25.0  # Turn on heater below this temperature
TEMP_MAX = 28.0  # Turn off heater above this temperature
HUMIDITY_MIN = 60.0  # Turn on humidifier below this humidity
HUMIDITY_MAX = 80.0  # Turn off humidifier above this humidity

# Sensor reading intervals
SENSOR_READ_INTERVAL = 10  # seconds
CONTROL_CHECK_INTERVAL = 5  # seconds
DATA_LOG_INTERVAL = 60     # seconds

@dataclass
class SensorData:
    temperature: float
    humidity: float
    timestamp: datetime
    
@dataclass
class RelayStatus:
    humidifier: bool = False
    water_pump: bool = False
    aquarium_pump: bool = False
    heater: bool = False

class EnvironmentalController:
    def __init__(self):
        self.sensor_data = SensorData(0.0, 0.0, datetime.now())
        self.relay_status = RelayStatus()
        self.esp32_last_contact = None
        self.running = True
        
        # Initialize GPIO
        self.setup_gpio()
        
        # Initialize database
        self.init_database()
        
        # Start background threads
        self.start_background_threads()
        
        logger.info("Environmental Controller initialized successfully")
    
    def setup_gpio(self):
        """Initialize GPIO pins for relay control"""
        GPIO.setmode(GPIO.BCM)
        GPIO.setwarnings(False)
        
        # Setup relay pins (LOW = relay ON, HIGH = relay OFF for most relay modules)
        for relay_name, pin in RELAY_PINS.items():
            GPIO.setup(pin, GPIO.OUT)
            GPIO.output(pin, GPIO.HIGH)  # Turn off all relays initially
            logger.info(f"Initialized {relay_name} relay on pin {pin}")
    
    def init_database(self):
        """Initialize SQLite database for data logging"""
        conn = sqlite3.connect('environmental_data.db')
        cursor = conn.cursor()
        
        # Create tables
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS sensor_readings (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                timestamp DATETIME,
                temperature REAL,
                humidity REAL,
                heater_status BOOLEAN,
                humidifier_status BOOLEAN,
                water_pump_status BOOLEAN,
                aquarium_pump_status BOOLEAN
            )
        ''')
        
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS esp32_commands (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                timestamp DATETIME,
                command TEXT,
                water_state TEXT,
                min_sensor INTEGER,
                max_sensor INTEGER
            )
        ''')
        
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS system_events (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                timestamp DATETIME,
                event_type TEXT,
                description TEXT,
                data TEXT
            )
        ''')
        
        conn.commit()
        conn.close()
        logger.info("Database initialized successfully")
    
    def start_background_threads(self):
        """Start background monitoring threads"""
        # Sensor reading thread
        sensor_thread = threading.Thread(target=self.sensor_monitoring_loop, daemon=True)
        sensor_thread.start()
        
        # Environmental control thread
        control_thread = threading.Thread(target=self.environmental_control_loop, daemon=True)
        control_thread.start()
        
        # Data logging thread
        logging_thread = threading.Thread(target=self.data_logging_loop, daemon=True)
        logging_thread.start()
        
        logger.info("Background threads started")
    
    def read_dht22(self) -> Optional[tuple]:
        """Read temperature and humidity from DHT22 sensor"""
        try:
            humidity, temperature = Adafruit_DHT.read_retry(Adafruit_DHT.DHT22, DHT22_PIN)
            if humidity is not None and temperature is not None:
                return temperature, humidity
            else:
                logger.warning("Failed to read DHT22 sensor")
                return None
        except Exception as e:
            logger.error(f"Error reading DHT22 sensor: {e}")
            return None
    
    def control_relay(self, relay_name: str, state: bool):
        """Control individual relay"""
        if relay_name not in RELAY_PINS:
            logger.error(f"Unknown relay: {relay_name}")
            return False
        
        pin = RELAY_PINS[relay_name]
        # For most relay modules: LOW = ON, HIGH = OFF
        gpio_state = GPIO.LOW if state else GPIO.HIGH
        GPIO.output(pin, gpio_state)
        
        # Update relay status
        setattr(self.relay_status, relay_name.lower(), state)
        
        action = "ON" if state else "OFF"
        logger.info(f"{relay_name} turned {action}")
        
        # Log system event
        self.log_system_event("RELAY_CONTROL", f"{relay_name} turned {action}")
        
        return True
    
    def sensor_monitoring_loop(self):
        """Background thread for continuous sensor monitoring"""
        while self.running:
            try:
                reading = self.read_dht22()
                if reading:
                    temperature, humidity = reading
                    self.sensor_data = SensorData(
                        temperature=temperature,
                        humidity=humidity,
                        timestamp=datetime.now()
                    )
                    
                    logger.info(f"Sensor reading - Temp: {temperature:.1f}°C, Humidity: {humidity:.1f}%")
                
                time.sleep(SENSOR_READ_INTERVAL)
                
            except Exception as e:
                logger.error(f"Error in sensor monitoring loop: {e}")
                time.sleep(SENSOR_READ_INTERVAL)
    
    def environmental_control_loop(self):
        """Background thread for automatic environmental control"""
        while self.running:
            try:
                # Temperature control
                if self.sensor_data.temperature < TEMP_MIN and not self.relay_status.heater:
                    self.control_relay('HEATER', True)
                    logger.info(f"Heater turned ON - Temperature: {self.sensor_data.temperature:.1f}°C")
                
                elif self.sensor_data.temperature >= TEMP_MAX and self.relay_status.heater:
                    self.control_relay('HEATER', False)
                    logger.info(f"Heater turned OFF - Temperature: {self.sensor_data.temperature:.1f}°C")
                
                # Humidity control
                if self.sensor_data.humidity < HUMIDITY_MIN and not self.relay_status.humidifier:
                    self.control_relay('HUMIDIFIER', True)
                    logger.info(f"Humidifier turned ON - Humidity: {self.sensor_data.humidity:.1f}%")
                
                elif self.sensor_data.humidity >= HUMIDITY_MAX and self.relay_status.humidifier:
                    self.control_relay('HUMIDIFIER', False)
                    logger.info(f"Humidifier turned OFF - Humidity: {self.sensor_data.humidity:.1f}%")
                
                time.sleep(CONTROL_CHECK_INTERVAL)
                
            except Exception as e:
                logger.error(f"Error in environmental control loop: {e}")
                time.sleep(CONTROL_CHECK_INTERVAL)
    
    def data_logging_loop(self):
        """Background thread for data logging"""
        while self.running:
            try:
                self.log_sensor_data()
                time.sleep(DATA_LOG_INTERVAL)
            except Exception as e:
                logger.error(f"Error in data logging loop: {e}")
                time.sleep(DATA_LOG_INTERVAL)
    
    def log_sensor_data(self):
        """Log sensor data to database"""
        try:
            conn = sqlite3.connect('environmental_data.db')
            cursor = conn.cursor()
            
            cursor.execute('''
                INSERT INTO sensor_readings 
                (timestamp, temperature, humidity, heater_status, humidifier_status, 
                 water_pump_status, aquarium_pump_status)
                VALUES (?, ?, ?, ?, ?, ?, ?)
            ''', (
                self.sensor_data.timestamp,
                self.sensor_data.temperature,
                self.sensor_data.humidity,
                self.relay_status.heater,
                self.relay_status.humidifier,
                self.relay_status.water_pump,
                self.relay_status.aquarium_pump
            ))
            
            conn.commit()
            conn.close()
            
        except Exception as e:
            logger.error(f"Error logging sensor data: {e}")
    
    def log_system_event(self, event_type: str, description: str, data: str = None):
        """Log system events to database"""
        try:
            conn = sqlite3.connect('environmental_data.db')
            cursor = conn.cursor()
            
            cursor.execute('''
                INSERT INTO system_events (timestamp, event_type, description, data)
                VALUES (?, ?, ?, ?)
            ''', (datetime.now(), event_type, description, data))
            
            conn.commit()
            conn.close()
            
        except Exception as e:
            logger.error(f"Error logging system event: {e}")
    
    def handle_esp32_command(self, command_data: Dict[str, Any]) -> Dict[str, Any]:
        """Handle commands from ESP32 water level monitor"""
        try:
            self.esp32_last_contact = datetime.now()
            
            water_state = command_data.get('water_state', 'UNKNOWN')
            pump_command = command_data.get('pump_command', 'NONE')
            
            logger.info(f"ESP32 Command - Water State: {water_state}, Pump Command: {pump_command}")
            
            # Log ESP32 command
            conn = sqlite3.connect('environmental_data.db')
            cursor = conn.cursor()
            cursor.execute('''
                INSERT INTO esp32_commands 
                (timestamp, command, water_state, min_sensor, max_sensor)
                VALUES (?, ?, ?, ?, ?)
            ''', (
                datetime.now(),
                pump_command,
                water_state,
                command_data.get('min_sensor', 0),
                command_data.get('max_sensor', 0)
            ))
            conn.commit()
            conn.close()
            
            # Execute pump command
            response = {"status": "success", "message": "Command processed"}
            
            if pump_command == "ON":
                if self.control_relay('AQUARIUM_PUMP', True):
                    response["message"] = "Aquarium pump turned ON"
                else:
                    response["status"] = "error"
                    response["message"] = "Failed to turn on aquarium pump"
            
            elif pump_command == "OFF":
                if self.control_relay('AQUARIUM_PUMP', False):
                    response["message"] = "Aquarium pump turned OFF"
                else:
                    response["status"] = "error"
                    response["message"] = "Failed to turn off aquarium pump"
            
            return response
            
        except Exception as e:
            logger.error(f"Error handling ESP32 command: {e}")
            return {"status": "error", "message": str(e)}
    
    def get_system_status(self) -> Dict[str, Any]:
        """Get current system status for mobile app"""
        return {
            "timestamp": datetime.now().isoformat(),
            "sensor_data": {
                "temperature": self.sensor_data.temperature,
                "humidity": self.sensor_data.humidity,
                "last_reading": self.sensor_data.timestamp.isoformat()
            },
            "relay_status": {
                "humidifier": self.relay_status.humidifier,
                "water_pump": self.relay_status.water_pump,
                "aquarium_pump": self.relay_status.aquarium_pump,
                "heater": self.relay_status.heater
            },
            "thresholds": {
                "temperature_min": TEMP_MIN,
                "temperature_max": TEMP_MAX,
                "humidity_min": HUMIDITY_MIN,
                "humidity_max": HUMIDITY_MAX
            },
            "esp32_status": {
                "last_contact": self.esp32_last_contact.isoformat() if self.esp32_last_contact else None,
                "connected": self.is_esp32_connected()
            }
        }
    
    def is_esp32_connected(self) -> bool:
        """Check if ESP32 is connected (received data in last 2 minutes)"""
        if not self.esp32_last_contact:
            return False
        
        time_diff = datetime.now() - self.esp32_last_contact
        return time_diff.total_seconds() < 120  # 2 minutes
    
    def manual_relay_control(self, relay_name: str, state: bool) -> Dict[str, Any]:
        """Manual relay control from mobile app"""
        try:
            if self.control_relay(relay_name.upper(), state):
                return {
                    "status": "success",
                    "message": f"{relay_name} turned {'ON' if state else 'OFF'}"
                }
            else:
                return {
                    "status": "error",
                    "message": f"Failed to control {relay_name}"
                }
        except Exception as e:
            return {"status": "error", "message": str(e)}
    
    def get_historical_data(self, hours: int = 24) -> Dict[str, Any]:
        """Get historical sensor data for mobile app"""
        try:
            conn = sqlite3.connect('environmental_data.db')
            cursor = conn.cursor()
            
            cursor.execute('''
                SELECT timestamp, temperature, humidity, heater_status, humidifier_status
                FROM sensor_readings
                WHERE timestamp > datetime('now', '-{} hours')
                ORDER BY timestamp DESC
            '''.format(hours))
            
            data = cursor.fetchall()
            conn.close()
            
            return {
                "status": "success",
                "data": [
                    {
                        "timestamp": row[0],
                        "temperature": row[1],
                        "humidity": row[2],
                        "heater_status": row[3],
                        "humidifier_status": row[4]
                    }
                    for row in data
                ]
            }
            
        except Exception as e:
            logger.error(f"Error getting historical data: {e}")
            return {"status": "error", "message": str(e)}
    
    def cleanup(self):
        """Cleanup GPIO and stop threads"""
        self.running = False
        GPIO.cleanup()
        logger.info("System cleanup completed")

# Flask Web API for mobile app communication
app = Flask(__name__)
controller = EnvironmentalController()

@app.route('/status', methods=['GET'])
def get_status():
    """Get current system status"""
    return jsonify(controller.get_system_status())

@app.route('/pump', methods=['POST'])
def handle_pump_command():
    """Handle pump commands from ESP32"""
    try:
        data = request.get_json()
        response = controller.handle_esp32_command(data)
        return jsonify(response)
    except Exception as e:
        logger.error(f"Error in pump command endpoint: {e}")
        return jsonify({"status": "error", "message": str(e)}), 500

@app.route('/control', methods=['POST'])
def manual_control():
    """Manual relay control from mobile app"""
    try:
        data = request.get_json()
        relay_name = data.get('relay')
        state = data.get('state')
        
        if not relay_name or state is None:
            return jsonify({"status": "error", "message": "Missing relay or state parameter"}), 400
        
        response = controller.manual_relay_control(relay_name, state)
        return jsonify(response)
        
    except Exception as e:
        logger.error(f"Error in manual control endpoint: {e}")
        return jsonify({"status": "error", "message": str(e)}), 500

@app.route('/history', methods=['GET'])
def get_history():
    """Get historical data"""
    try:
        hours = request.args.get('hours', 24, type=int)
        response = controller.get_historical_data(hours)
        return jsonify(response)
    except Exception as e:
        logger.error(f"Error in history endpoint: {e}")
        return jsonify({"status": "error", "message": str(e)}), 500

@app.route('/thresholds', methods=['POST'])
def update_thresholds():
    """Update temperature and humidity thresholds"""
    try:
        data = request.get_json()
        global TEMP_MIN, TEMP_MAX, HUMIDITY_MIN, HUMIDITY_MAX
        
        if 'temp_min' in data:
            TEMP_MIN = float(data['temp_min'])
        if 'temp_max' in data:
            TEMP_MAX = float(data['temp_max'])
        if 'humidity_min' in data:
            HUMIDITY_MIN = float(data['humidity_min'])
        if 'humidity_max' in data:
            HUMIDITY_MAX = float(data['humidity_max'])
        
        logger.info(f"Thresholds updated - Temp: {TEMP_MIN}-{TEMP_MAX}°C, Humidity: {HUMIDITY_MIN}-{HUMIDITY_MAX}%")
        
        return jsonify({
            "status": "success",
            "message": "Thresholds updated successfully",
            "thresholds": {
                "temp_min": TEMP_MIN,
                "temp_max": TEMP_MAX,
                "humidity_min": HUMIDITY_MIN,
                "humidity_max": HUMIDITY_MAX
            }
        })
        
    except Exception as e:
        logger.error(f"Error updating thresholds: {e}")
        return jsonify({"status": "error", "message": str(e)}), 500

if __name__ == '__main__':
    try:
        logger.info("Starting Environmental Control System")
        logger.info("=" * 50)
        logger.info("System Configuration:")
        logger.info(f"Temperature Control: {TEMP_MIN}°C - {TEMP_MAX}°C")
        logger.info(f"Humidity Control: {HUMIDITY_MIN}% - {HUMIDITY_MAX}%")
        logger.info(f"DHT22 Sensor Pin: {DHT22_PIN}")
        logger.info(f"Relay Pins: {RELAY_PINS}")
        logger.info("=" * 50)
        
        # Start Flask web server
        app.run(host='0.0.0.0', port=8080, debug=False, threaded=True)
        
    except KeyboardInterrupt:
        logger.info("Shutting down system...")
        controller.cleanup()
    except Exception as e:
        logger.error(f"System error: {e}")
        controller.cleanup() 