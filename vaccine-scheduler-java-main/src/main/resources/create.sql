CREATE TABLE Caregivers (
    Username varchar(255),
    Salt BINARY(16),
    Hash BINARY(16),
    PRIMARY KEY (Username)
);

CREATE TABLE Availabilities (
    Time date,
    Username varchar(255) REFERENCES Caregivers,
    PRIMARY KEY (Time, Username)
);

CREATE TABLE Vaccines (
    Name varchar(255),
    Doses int,
    PRIMARY KEY (Name)
);

CREATE TABLE Patients (
    Username varchar(255),
    Salt BINARY(16),
    Hash BINARY(16),
    PRIMARY KEY (Username)
);

CREATE TABLE Appointments (
    Appointment_ID int,
    Appointment_time date,
    Patient_Username varchar(255) REFERENCES Patients(Username),
    Caregiver_Username varchar(255) REFERENCES Caregivers(Username),
    Vaccine_name varchar(255) REFERENCES Vaccines(Name),
    PRIMARY KEY(Appointment_ID)
);