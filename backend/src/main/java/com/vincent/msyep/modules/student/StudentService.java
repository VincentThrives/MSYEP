package com.vincent.msyep.modules.student;

import com.vincent.msyep.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class StudentService {

    private final StudentRepository repo;

    public StudentService(StudentRepository repo) {
        this.repo = repo;
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

    public Student update(String id, Student changes) {
        Student e = findById(id);
        e.setName(changes.getName());
        e.setPhone(changes.getPhone());
        e.setEmail(changes.getEmail());
        e.setCourse(changes.getCourse());
        e.setCenterId(changes.getCenterId());
        e.setZoneId(changes.getZoneId());
        e.setDistrict(changes.getDistrict());
        e.setTaluk(changes.getTaluk());
        e.setGramPanchayat(changes.getGramPanchayat());
        if (changes.getDocuments() != null) e.setDocuments(changes.getDocuments());
        e.setActive(changes.isActive());
        e.setUpdatedAt(Instant.now());
        return repo.save(e);
    }

    public void delete(String id) {
        if (!repo.existsById(id)) {
            throw new ResourceNotFoundException("Student not found: " + id);
        }
        repo.deleteById(id);
    }
}
