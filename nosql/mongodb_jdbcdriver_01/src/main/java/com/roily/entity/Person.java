package com.roily.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

@Data
@AllArgsConstructor
@NoArgsConstructor
public final class Person {

    private ObjectId id;
    private String name;
    private int age;
    private Address address;

}
