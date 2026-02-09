# 🤝 Contributing to Gradify

Thank you for your interest in contributing to Gradify! This guide will help you contribute content (lectures, quizzes, formulas) and code.

---

## 📝 Contributing Content

### Adding Lectures

1. **Prepare your lecture data**:
   - Collect YouTube video IDs
   - Organize by week/topic
   - Create descriptive titles

2. **Create JSON file**:

   ```json
   {
     "subject": "Your Subject",
     "lectures": [
       {
         "week": 1,
         "title": "Introduction",
         "videoId": "YouTube_Video_ID"
       }
     ]
   }
   ```

3. **Submit**:
   - Fork the repository
   - Add file to `data/lectures/Foundation/` or `data/lectures/Diploma/`
   - Update `data/index.json`
   - Create pull request

### Adding Quizzes

1. **Create quiz JSON**:

   ```json
   {
     "subject": "Subject Name",
     "questions": [
       {
         "question": "Your question?",
         "options": ["A", "B", "C", "D"],
         "answer": 0,
         "explanation": "Why this is correct"
       }
     ]
   }
   ```

2. **Encrypt (optional)**:

   ```bash
   python scripts/encrypt_quiz.py your_quiz.json
   ```

3. **Submit via pull request**

### Adding Formulas

Edit `data/formulas.json`:

```json
{
  "category": "Mathematics",
  "formulas": [
    {
      "name": "Quadratic Formula",
      "formula": "x = (-b ± √(b²-4ac)) / 2a",
      "description": "Solves ax² + bx + c = 0"
    }
  ]
}
```

---

## 💻 Contributing Code

### Setup Development Environment

1. Fork and clone
2. Follow [Build & Run Guide](BUILD_AND_RUN.md)
3. Create feature branch
4. Make changes
5. Test thoroughly
6. Submit pull request

### Code Style

- Follow Java conventions
- Add comments for complex logic
- Use meaningful variable names
- Keep methods focused and small

### Pull Request Guidelines

- Clear description of changes
- Reference related issues
- Include screenshots for UI changes
- Ensure all tests pass

---

## 🐛 Reporting Bugs

Use [GitHub Issues](https://github.com/gtxPrime/gradify/issues) with:

- Clear title
- Steps to reproduce
- Expected vs actual behavior
- Screenshots if applicable
- Device/Android version

---

## 💡 Feature Requests

We welcome feature ideas! Open an issue with:

- Clear description
- Use case
- Mockups if applicable

---

Thank you for contributing to Gradify! 🎉
