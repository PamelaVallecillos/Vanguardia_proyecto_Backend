package com.example.dat.consultation.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.dat.consultation.entity.ConsultationDocument;

@Repository
public interface ConsultationDocumentRepo extends JpaRepository<ConsultationDocument, Long> {
    
    List<ConsultationDocument> findByConsultationId(Long consultationId);
    
    void deleteByConsultationId(Long consultationId);
}
