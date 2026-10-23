package ui;

import model.User;

public interface UserChangeListener {
    void onUserChanged(User newUser);
}
