package ex.rr.tasklist.controller;

import ex.rr.tasklist.database.entity.Task;
import ex.rr.tasklist.database.entity.TaskList;
import ex.rr.tasklist.database.entity.User;
import ex.rr.tasklist.database.repository.TaskListRepository;
import ex.rr.tasklist.database.repository.TaskRepository;
import ex.rr.tasklist.database.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("unused")
@Controller
@ComponentScan
public class ApiController {

    private static final Logger logger = LoggerFactory.getLogger(ApiController.class);
    private List<String> userHash;

    @Autowired
    private TaskListRepository taskListRepository;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private UserRepository userRepository;

    @GetMapping("/")
    public ResponseEntity<String> root(
    ) {
        return ResponseEntity.status(HttpStatus.I_AM_A_TEAPOT).body("I'm a teapot.");
    }

    @PostMapping("/api/user/create")
    public ResponseEntity<User> createUser(
            @RequestHeader("hash") String hash,
            @RequestBody User user
    ) {
        try {
            if (!userRepository.findByUsername(user.getUsername()).isPresent()) {
                return ResponseEntity.status(HttpStatus.CREATED).body(userRepository.saveAndFlush(user));
            } else {
                return ResponseEntity.status(HttpStatus.FOUND).build();
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/api/user/id/{userId}")
    public ResponseEntity<User> getUserById(
            @RequestHeader("hash") String hash,
            @PathVariable("userId") Long userId
    ) {
        if (validateHeader(hash)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Optional<User> user = userRepository.findById(userId);
        return user.map(value -> ResponseEntity.status(HttpStatus.OK).body(value))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @GetMapping("/api/user/name/{username}")
    public ResponseEntity<User> getUserByName(
            @RequestHeader("hash") String hash,
            @PathVariable("username") String username
    ) {
        if (validateHeader(hash)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Optional<User> user = userRepository.findByUsername(username);
        return user.map(value -> ResponseEntity.status(HttpStatus.OK).body(value))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @GetMapping("/api/user/id/{userId}/delete")
    public ResponseEntity<User> deleteUserById(
            @RequestHeader("hash") String hash,
            @PathVariable("userId") Long userId
    ) {
        if (validateHeader(hash)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            userRepository.deleteById(userId);
            return ResponseEntity.status(HttpStatus.OK).build();
        } catch (Exception e) {
            logger.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/api/user/name/{username}/delete")
    public ResponseEntity<User> deleteUserByName(
            @RequestHeader("hash") String hash,
            @PathVariable("username") String username
    ) {
        if (validateHeader(hash)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isPresent()) {
            userRepository.deleteById(user.get().getId());
            return ResponseEntity.status(HttpStatus.OK).build();
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PostMapping("/api/taskList/create")
    public ResponseEntity<TaskList> createTaskList(
            @RequestHeader("hash") String hash,
            @RequestBody TaskList taskList
    ) {
        if (validateHeader(hash)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(taskListRepository.save(taskList));
        } catch (Exception e) {
            logger.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/api/taskList/get/id/{listId}")
    public ResponseEntity<TaskList> getTaskListById(
            @RequestHeader("hash") String hash,
            @PathVariable("listId") Long listId
    ) {
        if (validateHeader(hash)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Optional<TaskList> taskList = taskListRepository.findById(listId);
        return taskList.map(list -> ResponseEntity.status(HttpStatus.OK).body(list))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @GetMapping("/api/taskList/get/user/{username}")
    public ResponseEntity<List<TaskList>> getTaskListById(
            @RequestHeader("hash") String hash,
            @PathVariable("username") String username
    ) {
        if (validateHeader(hash)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        List<TaskList> taskList = taskListRepository.findAllByUser(username);
        if (!taskList.isEmpty()) {
            return ResponseEntity.status(HttpStatus.OK).body(taskList);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/api/taskList/{listId}/delete")
    public ResponseEntity<Void> deleteTaskListById(
            @RequestHeader("hash") String hash,
            @PathVariable("listId") Long listId
    ) {
        if (validateHeader(hash)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            taskListRepository.deleteById(listId);
            return ResponseEntity.status(HttpStatus.OK).build();
        } catch (Exception e) {
            logger.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/api/taskList/{listId}/share/{username}")
    public ResponseEntity<String> shareTaskList(
            @RequestHeader("hash") String hash,
            @PathVariable("listId") Long listId,
            @PathVariable("username") String username
    ) {
        if (validateHeader(hash)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Optional<TaskList> taskList = taskListRepository.findById(listId);
        Optional<User> user = userRepository.findByUsername(username);

        if (taskList.isPresent() && user.isPresent()) {
            TaskList tempTaskList = taskList.get();
            tempTaskList.getSharedWith().add(user.get());
            taskListRepository.save(tempTaskList);
            return ResponseEntity.status(HttpStatus.OK).build();
        } else {
            if (!taskList.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("TaskList not found");
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
            }
        }
    }

    @GetMapping("/api/taskList/{listId}/unShare/{username}")
    public ResponseEntity<String> unShareTaskList(
            @RequestHeader("hash") String hash,
            @PathVariable("listId") Long listId,
            @PathVariable("username") String username
    ) {
        if (validateHeader(hash)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Optional<TaskList> taskList = taskListRepository.findById(listId);
        Optional<User> user = userRepository.findByUsername(username);

        if (taskList.isPresent() && user.isPresent()) {
            TaskList tempTaskList = taskList.get();
            tempTaskList.getSharedWith().remove(user.get());
            taskListRepository.save(tempTaskList);
            return ResponseEntity.status(HttpStatus.OK).build();
        } else {
            if (!taskList.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("TaskList not found");
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
            }
        }
    }

    @PostMapping("/api/taskList/{listId}/task/add")
    public ResponseEntity<Task> addTask(
            @RequestHeader("hash") String hash,
            @PathVariable("listId") Long listId,
            @RequestBody Task task
    ) {
        if (validateHeader(hash)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Optional<TaskList> taskList = taskListRepository.findById(listId);
        if (taskList.isPresent()) {
            TaskList list1 = taskList.get();
            list1.getTasks().add(task);
            TaskList updatedTaskList = taskListRepository.save(list1);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Collections.max(updatedTaskList.getTasks(), Comparator.comparing(Task::getCreatedAt)));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/api/taskList/{listId}/task/getAll")
    public ResponseEntity<List<Task>> getTaskListTasks(
            @RequestHeader("hash") String hash,
            @PathVariable("listId") Long listId
    ) {
        if (validateHeader(hash)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.status(HttpStatus.OK).body(taskRepository.findAllByTaskListId(listId));
    }

    @GetMapping("/api/taskList/{listId}/task/{taskId}/delete")
    public ResponseEntity<Void> deleteTask(
            @RequestHeader("hash") String hash,
            @PathVariable("listId") Long listId,
            @PathVariable("taskId") Long taskId
    ) {
        if (validateHeader(hash)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Optional<Task> task = taskRepository.getTaskIfBelongsToList(listId, taskId);
        if (task.isPresent()) {
            taskRepository.deleteById(taskId);
            return ResponseEntity.status(HttpStatus.OK).build();
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/api/taskList/{listId}/task/{taskId}/completed/{completed}")
    public ResponseEntity<Void> toggleTaskCompleted(
            @RequestHeader("hash") String hash,
            @PathVariable("listId") Long listId,
            @PathVariable("taskId") Long taskId,
            @PathVariable boolean completed
    ) {
        if (validateHeader(hash)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Optional<Task> task = taskRepository.getTaskIfBelongsToList(listId, taskId);
        if (task.isPresent()) {
            Task tempTask = task.get();
            if (completed) {
                tempTask.setCompleted(true);
                tempTask.setCompletedAt(System.currentTimeMillis());
            } else {
                tempTask.setCompleted(false);
                tempTask.setCompletedAt(null);
            }
            tempTask.setUpdatedAt(System.currentTimeMillis());
            taskRepository.save(tempTask);
            return ResponseEntity.status(HttpStatus.OK).build();
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PostMapping("/api/taskList/{listId}/update")
    public ResponseEntity<Void> updateTaskList(
            @RequestHeader("hash") String hash,
            @PathVariable("listId") Long listId,
            @RequestBody TaskList taskList
    ) {
        if (validateHeader(hash)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (listId.equals(taskList.getId())) {
            if (taskListRepository.existsById(listId)) {
                taskList.setUpdatedAt(System.currentTimeMillis());
                taskListRepository.save(taskList);
                return ResponseEntity.status(HttpStatus.CREATED).build();
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @PostMapping("/api/taskList/{listId}/task/{taskId}/update")
    public ResponseEntity<Void> updateTaskListTask(
            @RequestHeader("hash") String hash,
            @PathVariable("listId") Long listId,
            @PathVariable("taskId") Long taskId,
            @RequestBody Task task
    ) {
        if (validateHeader(hash)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (taskId.equals(task.getId()) && taskRepository.getTaskIfBelongsToList(listId, taskId).isPresent()) {
            if (taskRepository.existsById(listId)) {
                task.setUpdatedAt(System.currentTimeMillis());
                taskRepository.save(task);
                return ResponseEntity.status(HttpStatus.CREATED).build();
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    private boolean validateHeader(String hash) {
        if (userHash == null) userHash = userRepository.findActiveTokens();
        return !userHash.contains(hash);
    }

}
