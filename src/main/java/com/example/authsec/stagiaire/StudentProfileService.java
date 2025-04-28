package com.example.authsec.stagiaire;

import com.example.authsec.user.User;
import com.example.authsec.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class StudentProfileService {

    @Autowired
    private StudentProfileRepository profileRepository;

    @Autowired
    private UserRepository userRepository;

    public StudentProfile createProfile(StudentProfile profile, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        profile.setUser(user);

        // Lier les sous-entités
        if (profile.getEducations() != null) {
            profile.getEducations().forEach(edu -> edu.setStudentProfile(profile));
        }
        if (profile.getSkills() != null) {
            profile.getSkills().forEach(skill -> skill.setStudentProfile(profile));
        }
        if (profile.getCertifications() != null) {
            profile.getCertifications().forEach(cert -> cert.setStudentProfile(profile));
        }
        if (profile.getExperiences() != null) {
            profile.getExperiences().forEach(exp -> exp.setStudentProfile(profile));
        }

        return profileRepository.save(profile);
    }

    public StudentProfile updateProfile(Integer id, StudentProfile updatedProfile, String userEmail) {
        StudentProfile existingProfile = profileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Profil non trouvé"));

        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (!existingProfile.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Action non autorisée");
        }

        existingProfile.setHeadline(updatedProfile.getHeadline());
        existingProfile.setSummary(updatedProfile.getSummary());
        existingProfile.setLocation(updatedProfile.getLocation());
        existingProfile.setPhone(updatedProfile.getPhone());

        // Mise à jour des fichiers
        if (updatedProfile.getCvPath() != null) {
            deleteFile(existingProfile.getCvPath());
            existingProfile.setCvPath(updatedProfile.getCvPath());
        }
        if (updatedProfile.getMotivationLetterPath() != null) {
            deleteFile(existingProfile.getMotivationLetterPath());
            existingProfile.setMotivationLetterPath(updatedProfile.getMotivationLetterPath());
        }
        if (updatedProfile.getProfilePicture() != null) {
            deleteFile(existingProfile.getProfilePicture());
            existingProfile.setProfilePicture(updatedProfile.getProfilePicture());
        }
        if (updatedProfile.getCoverPhoto() != null) {
            deleteFile(existingProfile.getCoverPhoto());
            existingProfile.setCoverPhoto(updatedProfile.getCoverPhoto());
        }

        // Mise à jour des collections associées
        existingProfile.getEducations().clear();
        existingProfile.getSkills().clear();
        existingProfile.getCertifications().clear();
        existingProfile.getExperiences().clear();

        if (updatedProfile.getEducations() != null) {
            updatedProfile.getEducations().forEach(edu -> {
                edu.setStudentProfile(existingProfile);
                existingProfile.getEducations().add(edu);
            });
        }
        if (updatedProfile.getSkills() != null) {
            updatedProfile.getSkills().forEach(skill -> {
                skill.setStudentProfile(existingProfile);
                existingProfile.getSkills().add(skill);
            });
        }
        if (updatedProfile.getCertifications() != null) {
            updatedProfile.getCertifications().forEach(cert -> {
                cert.setStudentProfile(existingProfile);
                existingProfile.getCertifications().add(cert);
            });
        }
        if (updatedProfile.getExperiences() != null) {
            updatedProfile.getExperiences().forEach(exp -> {
                exp.setStudentProfile(existingProfile);
                existingProfile.getExperiences().add(exp);
            });
        }

        return profileRepository.save(existingProfile);
    }

    public void uploadProfileFiles(StudentProfile profile, MultipartFile cvFile,
                                   MultipartFile letterFile, MultipartFile profilePicture,
                                   MultipartFile coverPhoto) throws Exception {

        String basePath = new File(".").getCanonicalPath();
        String uploadDir = basePath + File.separator + "uploads" + File.separator + "student";

        File dir = new File(uploadDir);
        if (!dir.exists()) dir.mkdirs();

        if (cvFile != null && !cvFile.isEmpty()) {
            String cvPath = uploadDir + "/cv_" + System.currentTimeMillis() + "_" + cvFile.getOriginalFilename();
            cvFile.transferTo(new File(cvPath));
            profile.setCvPath(cvPath);
        }

        if (letterFile != null && !letterFile.isEmpty()) {
            String letterPath = uploadDir + "/letter_" + System.currentTimeMillis() + "_" + letterFile.getOriginalFilename();
            letterFile.transferTo(new File(letterPath));
            profile.setMotivationLetterPath(letterPath);
        }

        if (profilePicture != null && !profilePicture.isEmpty()) {
            String profilePicPath = uploadDir + "/profile_" + System.currentTimeMillis() + "_" + profilePicture.getOriginalFilename();
            profilePicture.transferTo(new File(profilePicPath));
            profile.setProfilePicture(profilePicPath);
        }

        if (coverPhoto != null && !coverPhoto.isEmpty()) {
            String coverPhotoPath = uploadDir + "/cover_" + System.currentTimeMillis() + "_" + coverPhoto.getOriginalFilename();
            coverPhoto.transferTo(new File(coverPhotoPath));
            profile.setCoverPhoto(coverPhotoPath);
        }
    }

    public void deleteFile(String path) {
        if (path != null) {
            File file = new File(path);
            if (file.exists()) {
                file.delete();
            }
        }
    }

    public Optional<StudentProfile> getProfileByUserEmail(String email) {
        return profileRepository.findByUserEmail(email);
    }

    public Map<String, String> getProfileFilesByUserEmail(String email) {
        Optional<StudentProfile> optionalProfile = profileRepository.findByUserEmail(email);
        if (optionalProfile.isPresent()) {
            StudentProfile profile = optionalProfile.get();
            Map<String, String> files = new HashMap<>();
            files.put("cv", profile.getCvPath());
            files.put("motivationLetter", profile.getMotivationLetterPath());
            files.put("profilePicture", profile.getProfilePicture());
            files.put("coverPhoto", profile.getCoverPhoto());
            return files;
        } else {
            throw new RuntimeException("Profil non trouvé pour l'utilisateur : " + email);
        }
    }

    @Transactional
    public void deleteProfile(Integer id, String userEmail) {
        StudentProfile profile = profileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Profil non trouvé"));

        if (!profile.getUser().getEmail().equals(userEmail)) {
            throw new RuntimeException("Action non autorisée");
        }

        // Supprimer aussi les fichiers liés
        deleteFile(profile.getCvPath());
        deleteFile(profile.getMotivationLetterPath());
        deleteFile(profile.getProfilePicture());
        deleteFile(profile.getCoverPhoto());

        profileRepository.delete(profile);
        profileRepository.flush();
    }
    public File getCvFile(String userEmail) {
        Optional<StudentProfile> optionalProfile = profileRepository.findByUserEmail(userEmail);
        if (optionalProfile.isPresent()) {
            String cvPath = optionalProfile.get().getCvPath();
            if (cvPath != null) {
                return new File(cvPath);
            }
        }
        return null;
    }
    public File getCoverPhoto(String userEmail) {
        Optional<StudentProfile> optionalProfile = profileRepository.findByUserEmail(userEmail);
        if (optionalProfile.isPresent()) {
            String coverPath = optionalProfile.get().getCoverPhoto();
            if (coverPath != null) {
                return new File(coverPath);
            }
        }
        return null;
    }


    public File getLetterFile(String userEmail) {
        Optional<StudentProfile> optionalProfile = profileRepository.findByUserEmail(userEmail);
        if (optionalProfile.isPresent()) {
            String letterPath = optionalProfile.get().getMotivationLetterPath();
            if (letterPath != null) {
                return new File(letterPath);
            }
        }
        return null;
    }

    public File getProfilePictureFile(String userEmail) {
        Optional<StudentProfile> optionalProfile = profileRepository.findByUserEmail(userEmail);
        if (optionalProfile.isPresent()) {
            String picturePath = optionalProfile.get().getProfilePicture();
            if (picturePath != null) {
                return new File(picturePath);
            }
        }
        return null;
    }

    public File getCoverPhotoFile(String userEmail) {
        Optional<StudentProfile> optionalProfile = profileRepository.findByUserEmail(userEmail);
        if (optionalProfile.isPresent()) {
            String coverPath = optionalProfile.get().getCoverPhoto();
            if (coverPath != null) {
                return new File(coverPath);
            }
        }
        return null;
    }

}
