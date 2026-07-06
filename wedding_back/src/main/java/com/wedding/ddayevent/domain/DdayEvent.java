package com.wedding.ddayevent.domain;

import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tbl_dday_event")
@Getter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DdayEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long ddayId;

    private String memberEmail;

    private String title;

    private LocalDate eventDate;

    private String memo;

    public void changeTitle(String title) {
        this.title = title;
    }

    public void changeEventDate(LocalDate eventDate) {
        this.eventDate = eventDate;
    }

    public void changeMemo(String memo) {
        this.memo = memo;
    }

}
