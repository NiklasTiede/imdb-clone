package com.thecodinglab.imdbclone.service;

import com.thecodinglab.imdbclone.identity.api.UserPrincipal;
import java.io.InputStream;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

  List<String> storeProfilePhoto(MultipartFile file, UserPrincipal currentUser);

  String deleteProfilePhoto(UserPrincipal currentUser);

  List<String> storeMovieImage(MultipartFile file, Long movieId);

  String deleteMovieImage(Long movieId);

  String storeFile(InputStream file, int fileSize, String fileName, String contentType);

  void deleteFile(String imageName);

  void setUpBucket();
}
