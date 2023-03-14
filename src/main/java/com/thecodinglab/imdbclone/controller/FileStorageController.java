package com.thecodinglab.imdbclone.controller;

import com.thecodinglab.imdbclone.payload.MessageResponse;
import com.thecodinglab.imdbclone.security.CurrentUser;
import com.thecodinglab.imdbclone.security.UserPrincipal;
import com.thecodinglab.imdbclone.service.FileStorageService;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Validated
@RequestMapping(("/api/file-storage"))
public class FileStorageController {

  private final FileStorageService fileStorageService;

  public FileStorageController(FileStorageService fileStorageService) {
    this.fileStorageService = fileStorageService;
  }

  /**
   * Structure of URI: <b>/profile-photo/{accountId}_size_{width}x{height}.jpg</b>
   *
   * <p>Size is 500x500 (detail view) and 80x80 (AppBar)
   */
  @PostMapping("/profile-photo")
  @PreAuthorize("hasRole('USER')")
  public ResponseEntity<List<String>> storeUserProfilePhoto(
      @RequestParam("image") MultipartFile multipartFile,
      @Parameter(hidden = true) @CurrentUser UserPrincipal currentUser) {
    return new ResponseEntity<>(
        fileStorageService.storeProfilePhoto(multipartFile, currentUser.getId()),
        HttpStatus.CREATED);
  }

  @DeleteMapping("/profile-photo")
  @PreAuthorize("hasRole('USER')")
  public ResponseEntity<MessageResponse> deleteUserProfilePhoto(
      @Parameter(hidden = true) @CurrentUser UserPrincipal currentUser) {
    return new ResponseEntity<>(
        new MessageResponse(fileStorageService.deleteProfilePhoto(currentUser.getId())),
        HttpStatus.NO_CONTENT);
  }

  /**
   * Structure of URI: <b>/movies/{movieId}_size_{width}x{height}.jpg</b>
   *
   * <p>Size is 500x750 (detail view) and 80x120 (movie search)
   */
  @PostMapping("/movie/{movieId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<String>> storeMovieImage(
      @PathVariable Long movieId, @RequestParam("image") MultipartFile multipartFile) {
    return new ResponseEntity<>(
        fileStorageService.storeMovieImage(multipartFile, movieId), HttpStatus.CREATED);
  }

  @DeleteMapping("/movie/{movieId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<MessageResponse> deleteMovieImage(@PathVariable Long movieId) {
    return new ResponseEntity<>(
        new MessageResponse(fileStorageService.deleteMovieImage(movieId)), HttpStatus.NO_CONTENT);
  }
}
