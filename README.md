# 🏥 Clinic Management System (JavaFX)

A desktop-based application developed using **JavaFX** and **MySQL** to manage clinic operations such as doctors, patients, and appointments efficiently.

---

## 📋 Project Overview
This project aims to automate the basic operations of a clinic, including:
- Managing patients’ and doctors’ records.
- Scheduling and viewing appointments.
- Providing an easy-to-use graphical interface built with JavaFX.

---

## 🛠️ Technologies Used
- **JavaFX** — for building the graphical user interface.
- **MySQL** — for database storage.
- **Scene Builder** — for designing FXML interfaces.
- **NetBeans IDE** — for development and integration.
- **JDBC** — for connecting Java with MySQL database.

---

## 📁 Project Structure


---

## ⚙️ Requirements

Before running the project, make sure you have:
- ☕ **Java JDK 17 or newer**
- 🧱 **MySQL Server** (or XAMPP)
- 🧩 **JavaFX SDK** (if not bundled in your JDK)
- 🧰 **NetBeans IDE** or **IntelliJ IDEA**

---

## 🚀 How to Run

### 🖥️ Option 1: From NetBeans
1. Clone or download the project:
   ```bash
   git clone https://github.com/abutahabahaa/ClinicProject.git

   
### 💾 Option 2: From JAR
Go to the /dist folder.

Open a terminal in that folder and run:
``bash    java -jar ClinicProject.jar`


## 🗄️ Database Setup
### Create Database
CREATE DATABASE clinicdb;
USE clinicdb;
### Create Tables
CREATE TABLE doctors (
  id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100),
  specialty VARCHAR(100),
  email VARCHAR(100)
);

CREATE TABLE patients (
  id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100),
  age INT,
  gender VARCHAR(10),
  phone VARCHAR(20)
);

CREATE TABLE appointments (
  id INT AUTO_INCREMENT PRIMARY KEY,
  patient_id INT,
  doctor_id INT,
  date DATE,
  time TIME,
  FOREIGN KEY (patient_id) REFERENCES patients(id),
  FOREIGN KEY (doctor_id) REFERENCES doctors(id)
);
## Configure Database Connection
### Edit the file DBConnection.java:
private static final String URL = "jdbc:mysql://localhost:3306/clinicdb";
private static final String USER = "root";
private static final String PASSWORD = "";

## 📸 Screenshots
### | Login Page                 | Dashboard                          | Appointments                             |
| -------------------------- | ---------------------------------- | ---------------------------------------- |
| ![Login](assets/login.png) | ![Dashboard](assets/dashboard.png) | ![Appointments](assets/appointments.png) |

## 👨‍💻 Developer Information

- **👤 Name**: Bahaa Mohamed Zakaria Abutaha
- **🎓 Major**: Computer Science
- **🏫 University**: Islamic University of Gaza
- **📧 Email**: abutahabahaa6@gmail.com
- **💻 Languages**: Java, HTML, CSS, JavaScript
- **🧠 Trained at**: Top Tech Company

   ## 📜 License

### This project is licensed under the MIT License.
### You can freely use, modify, and distribute it for educational or personal purposes.


