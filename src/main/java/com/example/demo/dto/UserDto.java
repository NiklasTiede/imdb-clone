package com.example.demo.dto;

import java.util.Date;
import javax.validation.constraints.*;

public class UserDto {

  private long UserId;

  @NotNull(message = "Name cannot be null.")
  @NotBlank(message = "username is mandatory")
  private String username;

  private String password;

  //    @Email(message = "Enter a valid email address.")
  @Size(max = 10)
  private String email;

  @Size(min = 2, max = 35, message = "firstName must be 2-35 characters long.")
  private String firstName;

  private String lastName;

  //    @NotBlank(message = "Email is mandatory")
  private String Phone;

  @Past(message = "Date input is invalid for a birth date.")
  private Date dateOfBirth;

  @Override
  public String toString() {
    return "UserDto{"
        + "UserId="
        + UserId
        + ", username='"
        + username
        + '\''
        + ", password='"
        + password
        + '\''
        + ", email='"
        + email
        + '\''
        + ", firstName='"
        + firstName
        + '\''
        + ", lastName='"
        + lastName
        + '\''
        + ", Phone='"
        + Phone
        + '\''
        + ", dateOfBirth="
        + dateOfBirth
        + '}';
  }

  public long getUserId() {
    return UserId;
  }

  public void setUserId(long userId) {
    UserId = userId;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public String getPhone() {
    return Phone;
  }

  public void setPhone(String phone) {
    Phone = phone;
  }

  public Date getDateOfBirth() {
    return dateOfBirth;
  }

  public void setDateOfBirth(Date dateOfBirth) {
    this.dateOfBirth = dateOfBirth;
  }
}
