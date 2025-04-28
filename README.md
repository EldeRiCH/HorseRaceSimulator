# Horse Race Simulation

A Java application that lets you simulate a horse race in two modes:

1. **Terminal version** (Part 1) using `Horse.java`, `Race.java` and `RaceTest.java`.  
2. **Swing GUI version** (Part 2) using `Horse.java` and `RaceGUI.java`.

---

## Table of Contents

- [Description](#description)  
- [Features](#features)  
- [Prerequisites](#prerequisites)  
- [Installation](#installation)  
- [Running](#running)  
  - [Part 1: Terminal](#part-1-terminal)  
  - [Part 2: GUI](#part-2-gui)  
- [Usage](#usage)  

---

## Description

This project demonstrates an object-oriented horse race simulation in Java.  
- **Part 1** (terminal) draws a text track in your shell, moves each horse step by step and prints the winner (or if they all fall).  
- **Part 2** (Swing GUI) offers a graphical race track, real-time animation, terrain effects, betting odds, and live confidence displays.

---

## Features

- **Part 1 (Terminal)**  
  - Enter track length and number of lanes (2–6).  
  - Each horse has a name, symbol, confidence (0–1), can move, fall, and replay with adjusted confidence.  
- **Part 2 (GUI)**  
  - Choose lanes, track length (200–700 px), and terrain (Normal, Muddy, Icy).  
  - Configure each horse’s name and initial confidence via sliders.  
  - Live‐animated race with flipped 🐎 emojis, slip/trip events, and confidence changes.  
  - Place bets with odds inversely proportional to confidence.  
  - Pause on slip/trip, show messages, and payout calculations.

---

## Prerequisites

- Java Development Kit (JDK 8 or later)  
- A terminal / command prompt  
- (Optional) IDE such as IntelliJ IDEA, Eclipse, or NetBeans

---
## Running
### Part 1
- compile the classes: 
  javac Horse.java Race.java RaceTest.java
- run at the terminal: 
  java RaceTest

### Part 2
- compile the GUI classes:
  javac Horse.java RaceGUI.java
- run the Swing GUI:
  java RaceGUI

---

## Installation

1. **Clone or download** this repository:  
   ```bash
   git clone https://github.com/ADjehiche/horse-race-simulation.git
   cd horse-race-simulation

---
## Usage
### Terminal Mode
You’ll be prompted for:

Track length (integer steps).

Number of lanes (2–6).

Predefined horses are added and the race starts.

After it finishes, you can replay with y/n.

### GUI Mode
Select number of lanes, track length (px) and terrain from the dropdowns.

Enter each horse’s name and set its confidence slider (0–100 → 0.00–1.00).

Place your bet on a horse; see live odds in the dropdown.

Start the race: watch the animation, slip/trip events, and final payout & stats.
