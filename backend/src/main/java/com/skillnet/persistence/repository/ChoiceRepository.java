package com.skillnet.persistence.repository;

import com.skillnet.persistence.entity.core.Choice;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChoiceRepository extends JpaRepository<Choice, Long> {

    List<Choice> findByQuestion_IdOrderByIdAsc(Long questionId);
}
