nodes

backend/

│

├── src/

│   │

│   ├── controllers/

│   │

│   ├── routes/

│   │

│   ├── services/

│   │

│   ├── models/

│   │

│   ├── middleware/

│   │

│   ├── config/

│   │

│   ├── utils/

│   │

│   └── app.js

│

├── .env

├── .gitignore

├── package.json

└── server.js



==================================

Spring Boot                 Node

\------------------------------------------------

@Entity                      mongoose.Schema

@Id                          \_id

JPA/Hibernate                Mongoose

@Entity class                Model



\----------------------------------------

Spring                  Node

\--------------------------------

@Service                services/

Business logic          Business logic



============================================

in spring                      Node



&#x20;               	     Request

&#x09;			↓

&#x09;		  authMiddleware

&#x20;  				↓

HTTP Request 		   Controller

&#x20;↓

Spring Security Filter

&#x20;  ↓

Controller



middleware/ → Filters / Interceptors



=========================================



Spring Boot:



application.properties

application.yml

@Configuration





Node commonly has:

config/

&#x20;   db.js



Example:

mongoose.connect(process.env.MONGO\_URI);



And .env:



PORT=5000

MONGO\_URI=your\_connection\_string

JWT\_SECRET=your\_secret



========================================

app.js ==> apne spring ke main class jesa



=========================================

server.js → SpringApplication.run()

======================================

package.json → pom.xml



\----------------------

@RestControllerAdvice

&#x20;       ↓

Global error middleware

spring me diff clases of exception handling or node me yaha per hota middleware



&#x20;                 Spring                                                                                                  Node

|Client<br />   ↓<br />Embedded Tomcat<br />   ↓<br />DispatcherServlet<br />   ↓<br />@RestController<br />   ↓<br />@Service<br />   ↓<br />@Repository<br />   ↓<br />JPA/Hibernate<br />   ↓<br />Database|Client<br />   ↓<br />server.js<br />   ↓<br />app.js<br />   ↓<br />userRoutes.js<br />   ↓<br />userController.js<br />   ↓<br />userService.js<br />   ↓<br />User.js (Mongoose Model)<br />   ↓<br />MongoDB|
|-|-|







Route

"Where should this request go?"===> router.get("/:id", userController.getUser);



&#x20;       ↓



Controller

|const getUser = async (req, res) => {<br /><br />    const user = await userService.getUser(req.params.id);<br /><br />    res.json(user);<br />};|@GetMapping("/users/{id}")-=======> for node this is routes<br />             ↑<br />          routing<br />public User getUser(...) ===>in same controller for spring)   for node ==>this is separate controller|
|-|-|



"What should we do with this request?"



&#x20;       ↓



Service

"What business logic should be applied?"



&#x20;       ↓



Model

"How do we interact with the database?"





@GetMapping("/users/{id}")

&#x20;            ↑

&#x20;         routing









=============================--------------------------------------==================================--------------------------===================



Spring Boot                    Node.js

────────────────────────────────────────────

MySQL/PostgreSQL               MongoDB Atlas

JDBC Driver                    MongoDB Driver

JPA/Hibernate                  Mongoose

@Entity                        mongoose.Schema

Repository                     Mongoose Model

@Table                         Collection

Row                            Document

Column                         Field





\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*

student-backend/

│

├── src/

├── .env              ← NEW

├── server.js

├── package.json

└── package-lock.json





server.js

&#x20;  │

&#x20;  ├── dotenv loads .env

&#x20;  │

&#x20;  ├── connectDB()

&#x20;  │       ↓

&#x20;  │   MongoDB Atlas

&#x20;  │

&#x20;  └── app.listen()

&#x20;          ↓

&#x20;      Express Server









@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@



npm init -y   npm.cmd init -y

npm.cmd install express mongoose dotenv

npm install --save-dev nodemon



node server.js



==========================================

@entity class me jaise table banate the wese he node me
apan models me schema banate h table ka





==============================================

table name decided in mongo db by follow-

const Student = mongoose.model("Student", studentSchema);

here Student is name of table but moongose by default convert it to students plural with s



mongodb.net/test

&#x20;          ↑

&#x20;      database name



=============swagger=============

npm install swagger-jsdoc swagger-ui-express

