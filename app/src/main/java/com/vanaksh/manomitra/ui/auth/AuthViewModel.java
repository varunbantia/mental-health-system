package com.vanaksh.manomitra.ui.auth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.vanaksh.manomitra.data.repository.AuthRepository;
import com.vanaksh.manomitra.utils.RoleManager;

public class AuthViewModel extends ViewModel {
    private final AuthRepository repository;
    private final MutableLiveData<AuthState> authState = new MutableLiveData<>();

    public AuthViewModel() {
        this.repository = new AuthRepository();
    }

    public LiveData<AuthState> getAuthState() {
        return authState;
    }

    public void login(String email, String password) {
        authState.setValue(new AuthState(true, null, null)); // Loading
        repository.signIn(email, password).observeForever(result -> {
            if (result.startsWith("SUCCESS:")) {
                String role = result.substring("SUCCESS:".length());
                authState.setValue(new AuthState(false, role, null));
            } else if (result.startsWith("ERROR:")) {
                String error = result.substring("ERROR: ".length());
                authState.setValue(new AuthState(false, null, error));
            }
        });
    }

    public void register(String email, String password, String name) {
        authState.setValue(new AuthState(true, null, null)); // Loading
        repository.signUp(email, password, name).observeForever(result -> {
            if (result.startsWith("SUCCESS:")) {
                String role = result.substring("SUCCESS:".length());
                authState.setValue(new AuthState(false, role, null));
            } else if (result.startsWith("ERROR:")) {
                String error = result.substring("ERROR: ".length());
                authState.setValue(new AuthState(false, null, error));
            }
        });
    }

    public void checkUserSession() {
        if (repository.getCurrentUser() != null) {
            authState.setValue(new AuthState(true, null, null)); // Loading
            MutableLiveData<String> roleResult = new MutableLiveData<>();
            repository.fetchUserRole(repository.getCurrentUser().getUid(), roleResult);
            roleResult.observeForever(result -> {
                if (result.startsWith("SUCCESS:")) {
                    String role = result.substring("SUCCESS:".length());
                    authState.setValue(new AuthState(false, role, null));
                } else {
                    authState.setValue(new AuthState(false, null, "Session Expired"));
                }
            });
        }
    }

    public void logout() {
        repository.logout();
        authState.setValue(null);
    }

    public static class AuthState {
        public final boolean isLoading;
        public final String role;
        public final String error;

        public AuthState(boolean isLoading, String role, String error) {
            this.isLoading = isLoading;
            this.role = role;
            this.error = error;
        }
    }
}
