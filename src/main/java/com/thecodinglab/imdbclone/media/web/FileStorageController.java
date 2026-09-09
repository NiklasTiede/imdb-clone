package com.thecodinglab.imdbclone.media.web;

import com.thecodinglab.imdbclone.media.internal.MediaService;
import com.thecodinglab.imdbclone.shared.security.CurrentUser;
import com.thecodinglab.imdbclone.shared.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Validated
@RequestMapping(path = "/api/v{version}/media", version = "1")
public class FileStorageController {

  private final MediaService mediaService;

  public FileStorageController(MediaService mediaService) {
    this.mediaService = mediaService;
  }

  /**
   * Structure of URI: <b>/profile-photo/{imageUrlToken}_size_{width}x{height}.jpg</b>
   *
   * <p>Size is 800x800 (detail view) and 120x120 (AppBar)
   */
  @PostMapping(value = "/profile-photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
  @ResponseStatus(HttpStatus.CREATED)
  public ResponseEntity<List<String>> storeUserProfilePhoto(
      @RequestParam("image") MultipartFile multipartFile,
      @Parameter(hidden = true) @CurrentUser UserPrincipal currentUser) {
    return new ResponseEntity<>(
        mediaService.storeProfilePhoto(multipartFile, currentUser), HttpStatus.CREATED);
  }

  @DeleteMapping("/profile-photo")
  @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public ResponseEntity<Void> deleteUserProfilePhoto(
      @Parameter(hidden = true) @CurrentUser UserPrincipal currentUser) {
    mediaService.deleteProfilePhoto(currentUser);
    return ResponseEntity.noContent().build();
  }

  /**
   * Structure of URI: <b>/movies/posters/{imageUrlToken}_size_{width}x{height}.jpg</b>
   *
   * <p>Size is 600x900 (detail view) and 120x180 (movie search)
   */
  @PostMapping(value = "/movies/{movieId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.CREATED)
  public ResponseEntity<List<String>> storeMovieImage(
      @PathVariable Long movieId, @RequestParam("image") MultipartFile multipartFile) {
    return new ResponseEntity<>(
        mediaService.storeMovieImage(multipartFile, movieId), HttpStatus.CREATED);
  }

  @DeleteMapping("/movies/{movieId}")
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public ResponseEntity<Void> deleteMovieImage(@PathVariable Long movieId) {
    mediaService.deleteMovieImage(movieId);
    return ResponseEntity.noContent().build();
  }
}
