package com.vincent.msyep.common;

import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

@Service
public class CounterService {

    private final MongoTemplate mongo;

    public CounterService(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    /** Atomically increment and return the next value for the named sequence. */
    public long next(String name) {
        Counter c = mongo.findAndModify(
                Query.query(Criteria.where("_id").is(name)),
                new Update().inc("seq", 1),
                FindAndModifyOptions.options().returnNew(true).upsert(true),
                Counter.class);
        return c == null ? 1L : c.getSeq();
    }
}
