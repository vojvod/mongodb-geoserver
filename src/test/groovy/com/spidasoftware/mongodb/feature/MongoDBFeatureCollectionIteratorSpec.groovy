package com.spidasoftware.mongodb.feature

import com.mongodb.BasicDBList
import com.mongodb.BasicDBObject
import com.mongodb.DB
import com.mongodb.DBCursor
import com.mongodb.MongoClient
import com.mongodb.MongoClientURI
import com.mongodb.ServerAddress
import com.mongodb.util.JSON
import com.spidasoftware.mongodb.data.SpidaDbDataAccess
import org.geotools.data.Query
import org.geotools.feature.NameImpl
import org.geotools.util.logging.Logging
import org.opengis.feature.Feature
import org.opengis.feature.type.FeatureType
import org.opengis.filter.Filter
import spock.lang.Specification

import java.util.logging.Logger

class MongoDBFeatureCollectionIteratorSpec extends Specification {

    static final Logger log = Logging.getLogger(MongoDBFeatureCollectionIteratorSpec.class.getPackage().getName())

    FeatureType featureType
    DB database
    BasicDBObject locationJSON
    DBCursor dbCursor
    SpidaDbDataAccess spidaDbDataAccess
    BasicDBList jsonMapping
    String namespace = "http://spida/db"

    void setup() {
        locationJSON = JSON.parse(getClass().getResourceAsStream('/location.json').text)
        String host = System.getProperty("mongoHost")
        String port = System.getProperty("mongoPort")
        String databaseName = System.getProperty("mongoDatabase")
        def serverAddress = new ServerAddress(host, Integer.valueOf(port))
        MongoClient mongoClient = new MongoClient(serverAddress)
        jsonMapping = JSON.parse(getClass().getResourceAsStream('/mapping.json').text)
        spidaDbDataAccess = new SpidaDbDataAccess(namespace, host, port, databaseName, null, null, jsonMapping)
        database = mongoClient.getDB(databaseName)
        database.getCollection("locations").remove(new BasicDBObject("id", locationJSON.get("id")))
        database.getCollection("locations").insert(locationJSON)
        dbCursor = database.getCollection("locations").find(new BasicDBObject("id", locationJSON.get("id")))

        jsonMapping = JSON.parse(getClass().getResourceAsStream('/mapping.json').text)
        spidaDbDataAccess = new SpidaDbDataAccess(namespace, System.getProperty("mongoHost"), System.getProperty("mongoPort"), System.getProperty("mongoDatabase"), null, null, jsonMapping)
    }

    void cleanup() {
        database.getCollection("locations").remove(new BasicDBObject("id", locationJSON.get("id")))
    }

    void testGetLocation() {
        setup:
            BasicDBObject mapping = jsonMapping.find { it.typeName == "location" }
            Query query = new Query("location", Filter.INCLUDE)
            featureType = spidaDbDataAccess.getSchema(new NameImpl(namespace, "location"))
            def locationFeatureCollectionIterator = new MongoDBFeatureCollectionIterator(dbCursor, featureType, mapping, query)
        when:
            Feature feature = locationFeatureCollectionIterator.next()
        then:
            !locationFeatureCollectionIterator.hasNext()
            feature.attributeCount == 17
            feature.getAttribute("id") == "55fac7fde4b0e7f2e3be342c"
            feature.getAttribute("label") == "684704E"
            feature.getAttribute("projectId") == "55fac7fde4b0e7f2e3be344f"
            feature.getAttribute("projectName") == "IJUS-44-2015-08-26-053"
            feature.getAttribute("dateModified") == 1442498557079
            feature.getAttribute("clientFile") == "SCE.client"
            feature.getAttribute("clientFileVersion") == "6ee5fba14760878be22701e1b3b7c05b"
            feature.getAttribute("mapNumber") == "ROME AVE."
            feature.getAttribute("comments") == "Two transformers connected to lower two cross arms"
            feature.getAttribute("streetNumber") == "8812"
            feature.getAttribute("street") == "Eberhart Rd NW"
            feature.getAttribute("city") == "Bolivar"
            feature.getAttribute("county") == "Tuscarawas"
            feature.getAttribute("state") == "OH"
            feature.getAttribute("zipCode") == "44622"
            feature.getAttribute("user") == "test@test.com"
            feature.getAttribute("geographicCoordinate").coordinate.y == 33.80541229248047
            feature.getAttribute("geographicCoordinate").coordinate.x == -118.3824234008789
    }

    void testLimitLocationPropertyNames() {
        setup:
            BasicDBObject mapping = jsonMapping.find { it.typeName == "location" }
            Query query = new Query("location", Filter.INCLUDE, ["id", "label"] as String[])
            featureType = spidaDbDataAccess.getSchema(new NameImpl(namespace, "location"))
            def locationFeatureCollectionIterator = new MongoDBFeatureCollectionIterator(dbCursor, featureType, mapping, query)
        when:
            Feature feature = locationFeatureCollectionIterator.next()
        then:
            !locationFeatureCollectionIterator.hasNext()
            feature.attributeCount == 17
            feature.getAttribute("id") == "55fac7fde4b0e7f2e3be342c"
            feature.getAttribute("label") == "684704E"
            feature.getAttribute("projectId") == null
            feature.getAttribute("projectName") == null
            feature.getAttribute("dateModified") == null
            feature.getAttribute("clientFile") == null
            feature.getAttribute("clientFileVersion") == null
            feature.getAttribute("mapNumber") == null
            feature.getAttribute("comments") == null
            feature.getAttribute("streetNumber") == null
            feature.getAttribute("street") == null
            feature.getAttribute("city") == null
            feature.getAttribute("county") == null
            feature.getAttribute("state") == null
            feature.getAttribute("zipCode") == null
            feature.getAttribute("user") == null
            feature.getAttribute("geographicCoordinate") == null
    }
}
