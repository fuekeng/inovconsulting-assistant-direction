package com.inovconsulting.assistant.repository;

import com.inovconsulting.assistant.models.Event;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Long> {

}
