package com.vincent.msyep.modules.student;

import com.vincent.msyep.common.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;

@Service
public class StudentService {

    private final StudentRepository repo;
    private final String uploadsDir;

    public StudentService(StudentRepository repo, @Value("${app.uploads-dir:uploads}") String uploadsDir) {
        this.repo = repo;
        this.uploadsDir = uploadsDir;
    }

    public List<Student> findAll() {
        return repo.findAll();
    }

    public List<Student> findByCenter(String centerId) {
        return repo.findByCenterId(centerId);
    }

    public Student findById(String id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + id));
    }

    public Student create(Student student) {
        student.setId(null);
        student.setCreatedAt(Instant.now());
        return repo.save(student);
    }

    public Student update(String id, Student c) {
        Student e = findById(id);
        e.setName(c.getName());
        e.setEmail(c.getEmail());
        e.setPhone(c.getPhone());
        e.setCourse(c.getCourse());
        // registerNo / batchCode / login immutable once generated.
        e.setGender(c.getGender());
        e.setDateOfBirth(c.getDateOfBirth());
        e.setCaste(c.getCaste());
        e.setEducationalQualification(c.getEducationalQualification());
        e.setAdmissionYear(c.getAdmissionYear());
        e.setInterestedInternship(c.getInterestedInternship());
        e.setTechnicalSkills(c.getTechnicalSkills());
        e.setHobbies(c.getHobbies());
        e.setInterestedCourses(c.getInterestedCourses());
        e.setCareerGoal(c.getCareerGoal());
        e.setCenterId(c.getCenterId());
        e.setZoneId(c.getZoneId());
        e.setState(c.getState());
        e.setDistrict(c.getDistrict());
        e.setTaluk(c.getTaluk());
        e.setGramPanchayat(c.getGramPanchayat());
        e.setPincode(c.getPincode());
        e.setPostalAddress(c.getPostalAddress());
        e.setNativePlace(c.getNativePlace());
        e.setHostelName(c.getHostelName());
        e.setCourseJoiningDate(c.getCourseJoiningDate());
        e.setCollegeName(c.getCollegeName());
        e.setActive(c.isActive());
        e.setUpdatedAt(Instant.now());
        return repo.save(e);
    }

    public void delete(String id) {
        if (!repo.existsById(id)) {
            throw new ResourceNotFoundException("Student not found: " + id);
        }
        repo.deleteById(id);
    }

    /** Save an uploaded student document to disk and attach it. */
    public Student attachDocument(String id, String type, String label, MultipartFile file) {
        Student s = findById(id);
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("No file provided");
        if (file.getSize() > 500 * 1024) throw new IllegalArgumentException("File exceeds 500 KB limit");
        try {
            Path dir = Paths.get(uploadsDir, "students", id);
            Files.createDirectories(dir);
            String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
            String safe = type + "_" + original.replaceAll("[^a-zA-Z0-9._-]", "_");
            file.transferTo(dir.resolve(safe).toAbsolutePath());
            s.getDocuments().removeIf(d -> type.equals(d.getType()));
            s.getDocuments().add(new StudentDocument(type, label, original, file.getSize(),
                    Paths.get("students", id, safe).toString()));
            return repo.save(s);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to store file: " + ex.getMessage());
        }
    }
}
