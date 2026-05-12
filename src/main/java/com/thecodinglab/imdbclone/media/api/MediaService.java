package com.thecodinglab.imdbclone.media.api;

import com.thecodinglab.imdbclone.security.UserPrincipal;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface MediaService {

  List<String> storeProfilePhoto(MultipartFile file, UserPrincipal currentUser);

  String deleteProfilePhoto(UserPrincipal currentUser);

  List<String> storeMovieImage(MultipartFile file, Long movieId);

  String deleteMovieImage(Long movieId);
}
