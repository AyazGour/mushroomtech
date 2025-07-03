#!/bin/bash

# Raspberry Pi Environmental Control System Setup Script
# Run this script to install all dependencies and configure the system

echo "=========================================="
echo "Raspberry Pi Environmental Control Setup"
echo "=========================================="

# Update system packages
echo "Updating system packages..."
sudo apt update
sudo apt upgrade -y

# Install Python3 and pip if not already installed
echo "Installing Python3 and pip..."
sudo apt install python3 python3-pip python3-venv -y

# Install system dependencies for DHT22 sensor
echo "Installing system dependencies..."
sudo apt install build-essential python3-dev -y

# Install GPIO libraries
echo "Installing GPIO libraries..."
sudo apt install python3-rpi.gpio -y

# Create virtual environment
echo "Creating virtual environment..."
python3 -m venv venv
source venv/bin/activate

# Install Python dependencies
echo "Installing Python dependencies..."
pip install --upgrade pip
pip install -r requirements.txt

# Install Adafruit DHT library manually (sometimes pip version doesn't work)
echo "Installing Adafruit DHT library..."
pip install Adafruit_DHT

# Create systemd service file for auto-start
echo "Creating systemd service..."
sudo tee /etc/systemd/system/environmental-control.service > /dev/null <<EOF
[Unit]
Description=Environmental Control System
After=network.target

[Service]
Type=simple
User=pi
WorkingDirectory=$(pwd)
Environment=PATH=$(pwd)/venv/bin
ExecStart=$(pwd)/venv/bin/python raspberry_pi_controller.py
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

# Enable and start the service
echo "Enabling systemd service..."
sudo systemctl daemon-reload
sudo systemctl enable environmental-control.service

# Create log directory
echo "Creating log directory..."
mkdir -p logs

# Set permissions
echo "Setting permissions..."
chmod +x raspberry_pi_controller.py
chmod +x setup_raspberry_pi.sh

echo "=========================================="
echo "Setup completed successfully!"
echo "=========================================="
echo ""
echo "Hardware Configuration:"
echo "- DHT22 sensor: Connect to GPIO pin 4"
echo "- 4-Channel Relay Module:"
echo "  - Relay 1 (Humidifier): GPIO pin 18"
echo "  - Relay 2 (Water Pump): GPIO pin 19"
echo "  - Relay 3 (Aquarium Pump): GPIO pin 20"
echo "  - Relay 4 (Heater): GPIO pin 21"
echo ""
echo "To start the system manually:"
echo "  source venv/bin/activate"
echo "  python raspberry_pi_controller.py"
echo ""
echo "To start the system as a service:"
echo "  sudo systemctl start environmental-control.service"
echo ""
echo "To check service status:"
echo "  sudo systemctl status environmental-control.service"
echo ""
echo "To view logs:"
echo "  sudo journalctl -u environmental-control.service -f"
echo ""
echo "API Endpoints available at http://[PI_IP]:8080"
echo "- GET  /status     - Get system status"
echo "- POST /pump       - Handle ESP32 commands"
echo "- POST /control    - Manual relay control"
echo "- GET  /history    - Get historical data"
echo "- POST /thresholds - Update temperature/humidity thresholds"
echo "==========================================" 