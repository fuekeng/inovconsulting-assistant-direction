package com.inovconsulting.assistant.repository;

import com.inovconsulting.assistant.model.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Long> {

}
