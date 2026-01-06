# The Drone Fleet Coordination Protocol (DFCP)

## Instructions to Compile and Run

### 1. Start the Mission Leader (Server)
- Run the `MainServer` class.
- This initializes the task status map and opens the UDP port (**4444**) to listen for drone registrations.

### 2. Deploy the Drones (Clients)
- Run the `MainClient` class. You can launch up to **5 instances** (multiple terminal tabs).
- Upon prompt, enter a **unique Drone ID** (e.g., `DR-10`, `DR-11`).

### 3. Monitor Progress
- Watch the console output or check the generated log files.
- The logs will display the distance-based time calculations and scanning progress for each task.

### 4. Simulate Failure
- Close a drone's terminal window to simulate a hardware crash or connection loss.

---

## Heartbeat & Task Reassignment Strategy

### 1. Heartbeat Logic
To ensure high availability, drones are equipped with a dedicated **Heartbeat Thread**.

- **Frequency:** A heartbeat signal is transmitted every **5 seconds**.
- **Server Monitoring:** For every registered drone, the server spawns a `DroneManager` thread that monitors these incoming signals.

### 2. Timeout Policy
- The server enforces a **10-second timeout window**.
- If the duration between the current time and the `lastHeartbeat` exceeds 10 seconds, the server officially marks the drone status as **LOST**.

### 3. Task Reassignment
- **Automatic Recycling:**  
  When a drone is marked LOST, the server immediately locates the task the drone was assigned to and reverts its status from `IN_PROGRESS` back to `PENDING`.
- **Dynamic Pickup:**  
  When an active drone sends a `REQUEST_TASK` signal, it will automatically be assigned the first available `PENDING` task by iterating the `taskStatus` map.

---

## Features

- **Multi-threaded Architecture**
  - `MissionLeaderServer` for central coordination.
  - Independent `DroneManager` threads for each drone.
  - Client-side `HeartbeatDrone` thread for concurrent signaling during task execution.
  - Client-side `ClientDrone` thread to manage `REGISTRATION`, `SUBMIT_RESULT`, and `REQUEST_TASK`.

- **UDP Communication**  
  Uses lightweight Datagram messaging for real-time telemetry and command delivery.

- **Automated Task Recovery**  
  Ensures **100% task completion** even in the event of multiple drone failures.

- **Physics-Based Simulation**  
  Task duration is calculated using the distance between GPS coordinates and a fixed velocity of **18 km/h**.

- **Aerial Reconnaissance**  
  Simulates image capture over a **3700 m radius** once the drone reaches the target geolocation.

- **FIFO Message Processing**  
  Adds a queue inside the `DroneManager` thread to process received data as **First-In-First-Out**.

---

## Known Limitations

- The current assignment logic depends on iterating through the task status array. This approach is efficient for this project but would be impractical for large-scale systems with hundreds of thousands of concurrent tasks.
- **Linear Flight Model:**  
  The simulation assumes a direct straight-line path (Euclidean distance) and does not account for obstacles, altitude changes, or wind resistance.
- **Hardcoded Constraints:**  
  The system is optimized for a fixed limit of **5 drones** and predefined task geolocations.

---

## Test Scenario Script

### Drone Setup
- Drone IDs: `DR-10`, `DR-11`, `DR-12`, `DR-13`, `DR-14`
- All drones default status set to = **AVAILABLE**

---

### Test 1: Multi-Threading & Concurrent Operations
**Objective:** Prove the system can handle multiple drones simultaneously without data corruption.

**Steps:**
1. Launch the `MainServer`.
2. Rapidly launch up to **5** different instances of `MainClient`.
3. Assign unique IDs to each by entering the ID from the keyboard.

**Expected Result:**  
The `MissionLeaderServer` should spawn up to 5 distinct `DroneManager` threads. The logs should show all drones receiving different tasks after their registration and then simulating scanning areas.

---

### Test 2: Heartbeat Failure & Drone Loss
**Objective:** Demonstrate that the server correctly identifies a drone as LOST when communication ceases.

**Steps:**
1. `DR-11` is actively scanning a task (during its `Thread.sleep` period).
2. Force-close its terminal window.
3. Observe the `MainServer` console.

**Expected Result:**  
After exactly **10 seconds** (the timeout threshold), the server should log:  
`Drone DR-11 status set to LOST`

---

### Test 3: Task Reassignment Mechanism
**Objective:** Ensure that a task assigned to a failed drone is not lost but recycled and reassigned.

**Steps:**
1. Take note of the Task ID assigned to `DR-11` (e.g., `TS-5`).  
   Server log example:  
   `Drone DR-11 got assigned to task TS-5 and set task TS-5 to IN_PROGRESS`
2. Kill one drone while others are active.
   - Expected log:  
     `"Task TS-5 set to PENDING because Drone DR-11 is LOST"`
3. Wait until one active drone becomes **AVAILABLE**.

**Expected Result:**  
The available drone will be assigned to task `TS-5`.

---

### Test 4: State Integrity Checks
**Objective:** Verify that the Mission Leader manages states correctly and rejects invalid operations.

**Case A: Duplicate ID**
- Register a new drone as `DR-12` while the original `DR-12` is active.
- Server log:  
  `Drone DR-12 tried to register again and rejects the new connection.`

**Case B: Invalid ID**
- Register a drone with ID `DR1`.
- Server log:  
  `Invalid droneId`

**Case C: Unregistered Traffic**
- Send any data from an unregistered drone.
- Server logs depend on the message:
  - `Request Task from unregistered drone {}`
  - `Result from unregistered drone {}`
  - `Heartbeat from unregistered drone {}`
