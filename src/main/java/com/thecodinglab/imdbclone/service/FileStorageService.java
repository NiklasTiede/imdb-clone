package com.thecodinglab.imdbclone.service;

import java.io.InputStream;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public interface FileStorageService {

  List<String> storeProfilePhoto(MultipartFile file, Long accountId);

  String deleteProfilePhoto(Long accountId);

  List<String> storeMovieImage(MultipartFile file, Long movieId);

  String deleteMovieImage(Long movieId);

  String storeFile(InputStream file, int fileSize, String fileName, String contentType);

  void deleteFile(String imageName);

  void setUpBucket();
}
