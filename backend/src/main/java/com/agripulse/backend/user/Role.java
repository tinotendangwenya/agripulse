package com.agripulse.backend.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "roles")
@Getter @Setter
public class Role {
    @Id
    private Byte id;

    @Column(nullable = false, unique = true, length = 30)
    private String name;
}
