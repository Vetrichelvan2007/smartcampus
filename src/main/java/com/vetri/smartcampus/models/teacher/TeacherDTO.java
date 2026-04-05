package com.vetri.smartcampus.models.teacher;

import java.sql.Date;
import java.sql.Timestamp;

public class TeacherDTO {

    private Long id;
    private String teacherClgId;
    private String name;
    private String email;
    private String phone;
    private String gender;
    private Date dateOfBirth;
    private String bloodGroup;
    private String address;

    private String designation;
    private String employmentType;
    private Date joiningDate;
    private int experienceYears;
    private String officeLocation;
    private String staffType;

    private String ugDegree;
    private String pgDegree;
    private String phdStatus;
    private String specialization;
    private String universityName;
    private int yearOfPassing;

    private int papersPublished;
    private int conferencesAttended;
    private int workshopsAttended;
    private int patents;
    private int fundedProjects;

    private int casualLeaveBalance;
    private int medicalLeaveBalance;
    private int earnedLeaveBalance;

    private String username;
    private String accountStatus;
    private Timestamp lastLogin;

    // ✅ Empty Constructor
    public TeacherDTO() {}

    // ---------------- GETTERS & SETTERS ----------------

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTeacherClgId() { return teacherClgId; }
    public void setTeacherClgId(String teacherClgId) { this.teacherClgId = teacherClgId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public Date getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(Date dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getBloodGroup() { return bloodGroup; }
    public void setBloodGroup(String bloodGroup) { this.bloodGroup = bloodGroup; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public String getEmploymentType() { return employmentType; }
    public void setEmploymentType(String employmentType) { this.employmentType = employmentType; }

    public Date getJoiningDate() { return joiningDate; }
    public void setJoiningDate(Date joiningDate) { this.joiningDate = joiningDate; }

    public int getExperienceYears() { return experienceYears; }
    public void setExperienceYears(int experienceYears) { this.experienceYears = experienceYears; }

    public String getOfficeLocation() { return officeLocation; }
    public void setOfficeLocation(String officeLocation) { this.officeLocation = officeLocation; }

    public String getStaffType() { return staffType; }
    public void setStaffType(String staffType) { this.staffType = staffType; }

    public String getUgDegree() { return ugDegree; }
    public void setUgDegree(String ugDegree) { this.ugDegree = ugDegree; }

    public String getPgDegree() { return pgDegree; }
    public void setPgDegree(String pgDegree) { this.pgDegree = pgDegree; }

    public String getPhdStatus() { return phdStatus; }
    public void setPhdStatus(String phdStatus) { this.phdStatus = phdStatus; }

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public String getUniversityName() { return universityName; }
    public void setUniversityName(String universityName) { this.universityName = universityName; }

    public int getYearOfPassing() { return yearOfPassing; }
    public void setYearOfPassing(int yearOfPassing) { this.yearOfPassing = yearOfPassing; }

    public int getPapersPublished() { return papersPublished; }
    public void setPapersPublished(int papersPublished) { this.papersPublished = papersPublished; }

    public int getConferencesAttended() { return conferencesAttended; }
    public void setConferencesAttended(int conferencesAttended) { this.conferencesAttended = conferencesAttended; }

    public int getWorkshopsAttended() { return workshopsAttended; }
    public void setWorkshopsAttended(int workshopsAttended) { this.workshopsAttended = workshopsAttended; }

    public int getPatents() { return patents; }
    public void setPatents(int patents) { this.patents = patents; }

    public int getFundedProjects() { return fundedProjects; }
    public void setFundedProjects(int fundedProjects) { this.fundedProjects = fundedProjects; }

    public int getCasualLeaveBalance() { return casualLeaveBalance; }
    public void setCasualLeaveBalance(int casualLeaveBalance) { this.casualLeaveBalance = casualLeaveBalance; }

    public int getMedicalLeaveBalance() { return medicalLeaveBalance; }
    public void setMedicalLeaveBalance(int medicalLeaveBalance) { this.medicalLeaveBalance = medicalLeaveBalance; }

    public int getEarnedLeaveBalance() { return earnedLeaveBalance; }
    public void setEarnedLeaveBalance(int earnedLeaveBalance) { this.earnedLeaveBalance = earnedLeaveBalance; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getAccountStatus() { return accountStatus; }
    public void setAccountStatus(String accountStatus) { this.accountStatus = accountStatus; }

    public Timestamp getLastLogin() { return lastLogin; }
    public void setLastLogin(Timestamp lastLogin) { this.lastLogin = lastLogin; }

}
