package com.att.tdp.issueflow.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "ticket_dependencies",
        uniqueConstraints = @UniqueConstraint(columnNames = {"ticket_id", "blocked_by_ticket_id"}))
public class TicketDependencyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private TicketEntity ticket;

    @ManyToOne(optional = false)
    @JoinColumn(name = "blocked_by_ticket_id", nullable = false)
    private TicketEntity blockedByTicket;

    protected TicketDependencyEntity() {}

    public TicketDependencyEntity(TicketEntity ticket, TicketEntity blockedByTicket) {
        this.ticket = ticket;
        this.blockedByTicket = blockedByTicket;
    }

    public Long getId() {
        return id;
    }

    public TicketEntity getTicket() {
        return ticket;
    }

    public TicketEntity getBlockedByTicket() {
        return blockedByTicket;
    }
}
