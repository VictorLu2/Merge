package com.pagerealm.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.HashSet;
import java.util.Set;

/**
 * `@Enumerated(EnumType.STRING)` 註解的作用是：
 * 在 JPA 實體類中，將枚舉型別（enum）以字串形式（而非預設的整數 ordinal）儲存到資料庫欄位。
 * 這樣資料庫會存 enum 的名稱（如 `ADMIN`），而不是序號（如 `0`），可讀性更高，也避免 enum 順序變動造成資料錯誤。
 */

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "roles")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private Integer roleId;

    @ToString.Exclude
    @Enumerated(EnumType.STRING)
    @Column(length = 20, name = "role_name")
    private AppRole roleName;

    @OneToMany(mappedBy = "role", fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    @JsonBackReference
    @ToString.Exclude
    private Set<User> users = new HashSet<>();

    public Role(AppRole roleName) {
        this.roleName = roleName;
    }


}
