<?php
header("Content-Type: application/json");

// Load environment variables
$env = parse_ini_file('/home2/palashfu/private/env.env');

if (!$env) {
    http_response_code(500);
    die(json_encode(["error" => "Failed to load environment variables"]));
}

// Database credentials
$db_host = $env['DB_HOST'];
$db_name = $env['DB_NAME'];
$db_user = $env['DB_USER'];
$db_pass = $env['DB_PASS'];

// Database connection
$conn = new mysqli($db_host, $db_user, $db_pass, $db_name);
if ($conn->connect_error) {
    http_response_code(500);
    die(json_encode(["error" => "Database connection failed"]));
}

// --- NOTES LOGIC ---
if (isset($_GET['type']) && $_GET['type'] === 'notes') {
    $subjectParam = isset($_GET['subject']) ? str_replace('_', ' ', $_GET['subject']) : null;

    if (!$subjectParam) {
        http_response_code(400);
        echo json_encode(["error" => "Missing subject parameter"]);
        exit;
    }

    $query = "SELECT link FROM notes WHERE subject = ?";
    $stmt = $conn->prepare($query);
    $stmt->bind_param("s", $subjectParam);
    $stmt->execute();
    $stmt->bind_result($noteLink);

    if ($stmt->fetch()) {
        echo json_encode(["link" => $noteLink, "subject" => $subjectParam]);
    } else {
        http_response_code(404);
        echo json_encode(["error" => "Notes not found for this subject"]);
    }
    exit;
}

// Normalize inputs (underscores to spaces)
$subject = isset($_GET['subject']) ? str_replace('_', ' ', $_GET['subject']) : null;
$quiz    = isset($_GET['quiz'])    ? str_replace('_', ' ', $_GET['quiz'])    : null;
$year    = isset($_GET['year'])    ? str_replace('_', ' ', $_GET['year'])    : null;
$session = isset($_GET['session']) ? str_replace('_', ' ', $_GET['session']) : null;

// 1. Subjects List
if (!$subject) {
    $query = "SELECT name FROM subjects";
    if ($stmt = $conn->prepare($query)) {
        $stmt->execute();
        $stmt->bind_result($name);
        $subjects = [];
        while ($stmt->fetch()) {
            $subjects[] = $name;
        }
        echo json_encode(["subjects" => $subjects]);
    } else {
        http_response_code(500);
        echo json_encode(["error" => "Database query failed"]);
    }
    exit;
}

// 2. Quiz Types for Subject
if (!$quiz) {
    $query = "SELECT qt.name FROM quiz_types qt 
              JOIN subjects s ON qt.subject_id = s.id 
              WHERE s.name = ?";
    $stmt = $conn->prepare($query);
    $stmt->bind_param("s", $subject);
    $stmt->execute();
    $stmt->bind_result($quiz_name);
    $quizzes = [];
    while ($stmt->fetch()) {
        $quizzes[] = $quiz_name;
    }
    if (empty($quizzes)) {
        // Technically not an error, just empty list, but could be 404 if subject invalid
        // Keeping as 200 OK with empty list for client handling
    }
    echo json_encode(["quizzes" => $quizzes]);
    exit;
}

// 3. Years for Quiz & Subject
if (!$year) {
    $query = "SELECT y.year FROM years y 
              JOIN quiz_types qt ON y.quiz_type_id = qt.id 
              JOIN subjects s ON qt.subject_id = s.id 
              WHERE qt.name = ? AND s.name = ?";
    $stmt = $conn->prepare($query);
    $stmt->bind_param("ss", $quiz, $subject);
    $stmt->execute();
    $stmt->bind_result($available_year);
    $years = [];
    while ($stmt->fetch()) {
        $years[] = $available_year;
    }
    echo json_encode(["years" => $years]);
    exit;
}

// 4. Sessions for Year, Quiz & Subject
if (!$session) {
    $query = "SELECT s.name FROM sessions s 
              JOIN years y ON s.year_id = y.id 
              JOIN quiz_types qt ON y.quiz_type_id = qt.id 
              JOIN subjects sub ON qt.subject_id = sub.id 
              WHERE y.year = ? AND qt.name = ? AND sub.name = ?";
    $stmt = $conn->prepare($query);
    $stmt->bind_param("sss", $year, $quiz, $subject);
    $stmt->execute();
    $stmt->bind_result($session_name);
    $sessions = [];
    while ($stmt->fetch()) {
        $sessions[] = $session_name;
    }
    echo json_encode(["sessions" => $sessions]);
    exit;
}

// 5. Final: Return paper links (matched with all 4 parameters)
$query = "SELECT qp.link FROM question_papers qp 
          JOIN sessions s ON qp.session_id = s.id 
          JOIN years y ON s.year_id = y.id 
          JOIN quiz_types qt ON y.quiz_type_id = qt.id 
          JOIN subjects sub ON qt.subject_id = sub.id 
          WHERE s.name = ? AND y.year = ? AND qt.name = ? AND sub.name = ?";
$stmt = $conn->prepare($query);
$stmt->bind_param("ssss", $session, $year, $quiz, $subject);
$stmt->execute();
$stmt->bind_result($paper_link);
$links = [];
while ($stmt->fetch()) {
    $links[] = $paper_link;
}

if (empty($links)) {
    http_response_code(404);
    echo json_encode(["error" => "No question papers found"]);
} else {
    echo json_encode(["links" => $links]);
}

$conn->close();
?>
