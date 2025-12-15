package com.example.dat.doctor.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.dat.doctor.entity.Schedule;

@Repository
public interface ScheduleRepo extends JpaRepository<Schedule, Long> {
    
    List<Schedule> findByDoctorId(Long doctorId);
    
    void deleteByDoctorId(Long doctorId);
}
