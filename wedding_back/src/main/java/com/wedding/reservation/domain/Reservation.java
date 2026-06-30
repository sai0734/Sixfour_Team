package com.wedding.reservation.domain;

import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tbl_reservation")
@Getter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Reservation {
  
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long reservationId;
  
  private String title;

  private String writer;

  private boolean complete;

  private LocalDate dueDate;

    public void changeTitle(String title){
    this.title = title;
  }

  public void changeComplete(boolean complete){
    this.complete = complete;
  }

  public void changeDueDate(LocalDate dueDate){
    this.dueDate = dueDate;
  }

}