#!/usr/bin/env python3
"""
Raspberry Pi Environmental Control System with Firebase Integration
- Controls 4-channel relay module for AC appliances
- Monitors temperature and humidity with DHT22
- Receives commands from ESP32 water level monitor via Firebase
- Provides cloud-based API for mobile application
- Automatic temperature and humidity control
"""

import RPi.GPIO as GPIO
import Adafruit_DHT
import time
import json
import threading
import logging
from datetime import datetime
import sqlite3
import requests
from dataclasses import dataclass
from typing import Dict, Any, Optional
import firebase_admin
from firebase_admin import credentials, db, firestore
import uuid

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

# Firebase Configuration
FIREBASE_CONFIG = {
    "type": "service_account",
    "project_id": "your-project-id",
    "private_key_id": "your-private-key-id",
    "private_key": "-----BEGIN PRIVATE KEY-----\nYOUR_PRIVATE_KEY_HERE\n-----END PRIVATE KEY-----\n",
    "client_email": "your-service-account@your-project-id.iam.gserviceaccount.com",
    "client_id": "your-client-id",
    "auth_uri": "https://accounts.google.com/o/oauth2/auth",
    "token_uri": "https://oauth2.googleapis.com/token",
    "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs",
    "client_x509_cert_url": "https://www.googleapis.com/robot/v1/metadata/x509/your-service-account%40your-project-id.iam.gserviceaccount.com"
}

DATABASE_URL = "https://your-project-id-default-rtdb.firebaseio.com/"

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
FIREBASE_UPDATE_INTERVAL = 5     # seconds

# Device ID for this Raspberry Pi
DEVICE_ID = f"rpi_{uuid.uuid4().hex[:8]}"

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

class FirebaseEnvironmentalController:
    def __init__(self):
        self.sensor_data = SensorData(0.0, 0.0, datetime.now())
        self.relay_status = RelayStatus()
        self.esp32_last_contact = None
        self.running = True
        self.firebase_app = None
        self.db_ref = None
        
        # Initialize Firebase
        self.init_firebase()
        
        # Initialize GPIO
        self.setup_gpio()
        
        # Initialize local database
        self.init_local_database()
        
        # Start background threads
        self.start_background_threads()
        
        # Setup Firebase listeners
        self.setup_firebase_listeners()
        
        logger.info("Firebase Environmental Controller initialized successfully")
    
    def init_firebase(self):
        """Initialize Firebase connection"""
        try:
            # Check if Firebase is already initialized
            try:
                firebase_admin.get_app()
                logger.info("Firebase already initialized")
            except ValueError:
                # Initialize Firebase
                cred = credentials.Certificate(FIREBASE_CONFIG)
                self.firebase_app = firebase_admin.initialize_app(cred, {
                    'databaseURL': DATABASE_URL
                })
                logger.info("Firebase initialized successfully")
            
            # Get database reference
            self.db_ref = db.reference()
            
            # Register this device
            self.register_device()
            
        except Exception as e:
            logger.error(f"Failed to initialize Firebase: {e}")
            raise
    
    def register_device(self):
        """Register this Raspberry Pi device in Firebase"""
        try:
            device_info = {
                'device_id': DEVICE_ID,
                'device_type': 'raspberry_pi',
                'status': 'online',
                'last_seen': datetime.now().isoformat(),
                'capabilities': {
                    'temperature_control': True,
                    'humidity_control': True,
                    'water_pump_control': True,
                    'relay_control': True
                },
                'gpio_pins': RELAY_PINS,
                'thresholds': {
                    'temp_min': TEMP_MIN,
                    'temp_max': TEMP_MAX,
                    'humidity_min': HUMIDITY_MIN,
                    'humidity_max': HUMIDITY_MAX
                }
            }
            
            self.db_ref.child('devices').child(DEVICE_ID).set(device_info)
            logger.info(f"Device {DEVICE_ID} registered in Firebase")
            
        except Exception as e:
            logger.error(f"Failed to register device: {e}")
    
    def setup_gpio(self):
        """Initialize GPIO pins for relay control"""
        GPIO.setmode(GPIO.BCM)
        GPIO.setwarnings(False)
        
        # Setup relay pins (LOW = relay ON, HIGH = relay OFF for most relay modules)
        for relay_name, pin in RELAY_PINS.items():
            GPIO.setup(pin, GPIO.OUT)
            GPIO.output(pin, GPIO.HIGH)  # Turn off all relays initially
            logger.info(f"Initialized {relay_name} relay on pin {pin}")
    
    def init_local_database(self):
        """Initialize SQLite database for local data logging"""
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
                aquarium_pump_status BOOLEAN,
                synced_to_firebase BOOLEAN DEFAULT 0
            )
        ''')
        
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS esp32_commands (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                timestamp DATETIME,
                command TEXT,
                water_state TEXT,
                min_sensor INTEGER,
                max_sensor INTEGER,
                synced_to_firebase BOOLEAN DEFAULT 0
            )
        ''')
        
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS system_events (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                timestamp DATETIME,
                event_type TEXT,
                description TEXT,
                data TEXT,
                synced_to_firebase BOOLEAN DEFAULT 0
            )
        ''')
        
        conn.commit()
        conn.close()
        logger.info("Local database initialized successfully")
    
    def setup_firebase_listeners(self):
        """Setup Firebase listeners for remote commands"""
        try:
            # Listen for relay control commands
            self.db_ref.child('commands').child(DEVICE_ID).child('relay_control').listen(self.handle_relay_command)
            
            # Listen for threshold updates
            self.db_ref.child('commands').child(DEVICE_ID).child('thresholds').listen(self.handle_threshold_update)
            
            # Listen for ESP32 commands
            self.db_ref.child('esp32_commands').listen(self.handle_esp32_command)
            
            logger.info("Firebase listeners setup successfully")
            
        except Exception as e:
            logger.error(f"Failed to setup Firebase listeners: {e}")
    
    def handle_relay_command(self, event):
        """Handle relay control commands from Firebase"""
        try:
            if event.data is None:
                return
            
            command_data = event.data
            relay_name = command_data.get('relay', '').upper()
            state = command_data.get('state', False)
            command_id = command_data.get('command_id', '')
            
            logger.info(f"Received relay command: {relay_name} -> {state}")
            
            if relay_name in RELAY_PINS:
                success = self.control_relay(relay_name, state)
                
                # Send response back to Firebase
                response = {
                    'command_id': command_id,
                    'success': success,
                    'timestamp': datetime.now().isoformat(),
                    'device_id': DEVICE_ID
                }
                
                self.db_ref.child('command_responses').child(command_id).set(response)
                
                # Clear the command
                self.db_ref.child('commands').child(DEVICE_ID).child('relay_control').delete()
            
        except Exception as e:
            logger.error(f"Error handling relay command: {e}")
    
    def handle_threshold_update(self, event):
        """Handle threshold update commands from Firebase"""
        try:
            if event.data is None:
                return
            
            global TEMP_MIN, TEMP_MAX, HUMIDITY_MIN, HUMIDITY_MAX
            
            threshold_data = event.data
            
            if 'temp_min' in threshold_data:
                TEMP_MIN = float(threshold_data['temp_min'])
            if 'temp_max' in threshold_data:
                TEMP_MAX = float(threshold_data['temp_max'])
            if 'humidity_min' in threshold_data:
                HUMIDITY_MIN = float(threshold_data['humidity_min'])
            if 'humidity_max' in threshold_data:
                HUMIDITY_MAX = float(threshold_data['humidity_max'])
            
            logger.info(f"Thresholds updated - Temp: {TEMP_MIN}-{TEMP_MAX}°C, Humidity: {HUMIDITY_MIN}-{HUMIDITY_MAX}%")
            
            # Update device info in Firebase
            self.db_ref.child('devices').child(DEVICE_ID).child('thresholds').update({
                'temp_min': TEMP_MIN,
                'temp_max': TEMP_MAX,
                'humidity_min': HUMIDITY_MIN,
                'humidity_max': HUMIDITY_MAX
            })
            
            # Clear the command
            self.db_ref.child('commands').child(DEVICE_ID).child('thresholds').delete()
            
        except Exception as e:
            logger.error(f"Error handling threshold update: {e}")
    
    def handle_esp32_command(self, event):
        """Handle commands from ESP32 water level monitor via Firebase"""
        try:
            if event.data is None:
                return
            
            for esp32_id, command_data in event.data.items():
                if command_data is None:
                    continue
                
                self.esp32_last_contact = datetime.now()
                
                water_state = command_data.get('water_state', 'UNKNOWN')
                pump_command = command_data.get('pump_command', 'NONE')
                
                logger.info(f"ESP32 Command from {esp32_id} - Water State: {water_state}, Pump Command: {pump_command}")
                
                # Log ESP32 command locally
                self.log_esp32_command_local(command_data)
                
                # Execute pump command
                if pump_command == "ON":
                    self.control_relay('AQUARIUM_PUMP', True)
                elif pump_command == "OFF":
                    self.control_relay('AQUARIUM_PUMP', False)
                
                # Send response back to Firebase
                response = {
                    'device_id': DEVICE_ID,
                    'timestamp': datetime.now().isoformat(),
                    'status': 'processed',
                    'action_taken': pump_command
                }
                
                self.db_ref.child('esp32_responses').child(esp32_id).set(response)
                
                # Clear the processed command
                self.db_ref.child('esp32_commands').child(esp32_id).delete()
            
        except Exception as e:
            logger.error(f"Error handling ESP32 command: {e}")
    
    def start_background_threads(self):
        """Start background monitoring threads"""
        # Sensor reading thread
        sensor_thread = threading.Thread(target=self.sensor_monitoring_loop, daemon=True)
        sensor_thread.start()
        
        # Environmental control thread
        control_thread = threading.Thread(target=self.environmental_control_loop, daemon=True)
        control_thread.start()
        
        # Firebase sync thread
        firebase_thread = threading.Thread(target=self.firebase_sync_loop, daemon=True)
        firebase_thread.start()
        
        # Local data logging thread
        logging_thread = threading.Thread(target=self.data_logging_loop, daemon=True)
        logging_thread.start()
        
        logger.info("Background threads started")
    
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
    
    def firebase_sync_loop(self):
        """Background thread for syncing data to Firebase"""
        while self.running:
            try:
                self.sync_to_firebase()
                time.sleep(FIREBASE_UPDATE_INTERVAL)
            except Exception as e:
                logger.error(f"Error in Firebase sync loop: {e}")
                time.sleep(FIREBASE_UPDATE_INTERVAL)
    
    def data_logging_loop(self):
        """Background thread for local data logging"""
        while self.running:
            try:
                self.log_sensor_data_local()
                time.sleep(60)  # Log every minute
            except Exception as e:
                logger.error(f"Error in data logging loop: {e}")
                time.sleep(60)
    
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
        self.log_system_event_local("RELAY_CONTROL", f"{relay_name} turned {action}")
        
        return True
    
    def sync_to_firebase(self):
        """Sync current status to Firebase"""
        try:
            current_status = {
                'device_id': DEVICE_ID,
                'timestamp': datetime.now().isoformat(),
                'sensor_data': {
                    'temperature': self.sensor_data.temperature,
                    'humidity': self.sensor_data.humidity,
                    'last_reading': self.sensor_data.timestamp.isoformat()
                },
                'relay_status': {
                    'humidifier': self.relay_status.humidifier,
                    'water_pump': self.relay_status.water_pump,
                    'aquarium_pump': self.relay_status.aquarium_pump,
                    'heater': self.relay_status.heater
                },
                'thresholds': {
                    'temp_min': TEMP_MIN,
                    'temp_max': TEMP_MAX,
                    'humidity_min': HUMIDITY_MIN,
                    'humidity_max': HUMIDITY_MAX
                },
                'esp32_status': {
                    'last_contact': self.esp32_last_contact.isoformat() if self.esp32_last_contact else None,
                    'connected': self.is_esp32_connected()
                },
                'system_info': {
                    'uptime': time.time(),
                    'status': 'online'
                }
            }
            
            # Update device status in Firebase
            self.db_ref.child('device_status').child(DEVICE_ID).set(current_status)
            
            # Update last seen
            self.db_ref.child('devices').child(DEVICE_ID).child('last_seen').set(datetime.now().isoformat())
            
        except Exception as e:
            logger.error(f"Error syncing to Firebase: {e}")
    
    def log_sensor_data_local(self):
        """Log sensor data to local database"""
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
    
    def log_esp32_command_local(self, command_data):
        """Log ESP32 command to local database"""
        try:
            conn = sqlite3.connect('environmental_data.db')
            cursor = conn.cursor()
            
            cursor.execute('''
                INSERT INTO esp32_commands 
                (timestamp, command, water_state, min_sensor, max_sensor)
                VALUES (?, ?, ?, ?, ?)
            ''', (
                datetime.now(),
                command_data.get('pump_command', 'NONE'),
                command_data.get('water_state', 'UNKNOWN'),
                command_data.get('min_sensor', 0),
                command_data.get('max_sensor', 0)
            ))
            
            conn.commit()
            conn.close()
            
        except Exception as e:
            logger.error(f"Error logging ESP32 command: {e}")
    
    def log_system_event_local(self, event_type: str, description: str, data: str = None):
        """Log system events to local database"""
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
    
    def is_esp32_connected(self) -> bool:
        """Check if ESP32 is connected (received data in last 2 minutes)"""
        if not self.esp32_last_contact:
            return False
        
        time_diff = datetime.now() - self.esp32_last_contact
        return time_diff.total_seconds() < 120  # 2 minutes
    
    def cleanup(self):
        """Cleanup GPIO and stop threads"""
        self.running = False
        
        # Update device status to offline
        try:
            self.db_ref.child('devices').child(DEVICE_ID).child('status').set('offline')
            self.db_ref.child('device_status').child(DEVICE_ID).child('system_info').child('status').set('offline')
        except:
            pass
        
        GPIO.cleanup()
        logger.info("System cleanup completed")

if __name__ == '__main__':
    controller = None
    try:
        logger.info("Starting Firebase Environmental Control System")
        logger.info("=" * 50)
        logger.info("System Configuration:")
        logger.info(f"Device ID: {DEVICE_ID}")
        logger.info(f"Temperature Control: {TEMP_MIN}°C - {TEMP_MAX}°C")
        logger.info(f"Humidity Control: {HUMIDITY_MIN}% - {HUMIDITY_MAX}%")
        logger.info(f"DHT22 Sensor Pin: {DHT22_PIN}")
        logger.info(f"Relay Pins: {RELAY_PINS}")
        logger.info(f"Firebase Database: {DATABASE_URL}")
        logger.info("=" * 50)
        
        controller = FirebaseEnvironmentalController()
        
        # Keep the main thread alive
        while True:
            time.sleep(1)
        
    except KeyboardInterrupt:
        logger.info("Shutting down system...")
        if controller:
            controller.cleanup()
    except Exception as e:
        logger.error(f"System error: {e}")
        if controller:
            controller.cleanup() 