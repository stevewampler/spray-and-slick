Overview
===

A twitter-like notification service written in Scala using Spray (HTTP Services on Akka), Slick (ORM for Scala), and Akka.

Main class: com.sgw.service.ServiceApp

By default, the service runs on localhost:8080 and uses a MySQL DB named "rest" at localhost:3306.


Operational Constraints
===

DB Performance
----

I chose to use MySQL to store the notifications (tweets). In addition to primary key (the id), table is index by user-id
for fast user-id lookups. I haven't measured it yet, but the DB should be able to handle thousands of transactions
per-second, but as a next step I would measure the actual TPS rate of DB alone and through the service under various types of loads.

If the database is a bottle neck, I'd consider changing the following:

* Partitions the notifications table based on the notification timestamp.
 * Use snowflake ids (https://github.com/twitter/snowflake) to combine the id and timestamp field into one primary key (id) field
 that can be partitioned.
* Change the DAO to only do appends (notification changes currently update existing records).
Edits to a notification would result in a new notification record that supersedes the old one.
* Switch to a distributed DB such as Amazon's Redshift, to distribute the DB load across multiple machines.
* Based on the actual queries, I'd also look at optimizing the table's indices.

I'd also consider putting the new notifications onto a Kafka stream to enable multiple persistence options to be
tried and used in parallel.

The DAO currently enables offset+limit queries, but the REST API does not.
 * As the table grows larger, a client will need a way to page through a user's notifications and the DAO will
 need a way to protect the DB from long running queries.
 * I'd also change the use of offset+limit queries: http://use-the-index-luke.com/no-offset
 
Service Reliability and Availability
----

The web service is currently a single application that uses multiple threads to service requests.
A single service will fail at some point or may become I/O bound. To address both possibilities, 
I'd deploy the service to multiple machines running behind a load-balancer and I'd
auto-scale the number of machines based on the actual load and perhaps on the load history. 


Credits
===

I used this exercise as a chance to play with Spray (HTTP Services on Akka), Slick (ORM for Scala),
and Dispatch (a Scala HTTP client library). The code is patterned after Oleg Yermolaiev's 
"Building REST Service with Scala" blog post at:

* http://sysgears.com/articles/building-rest-service-with-scala/
* https://github.com/oermolaev/simple-scala-rest-example
