(use 'capacitor.core)

(def client (make-client {:db "mynewdb"}))

; Update user password
(update-user-password client "myuser" "mynewpassword")

; Show user grants
(list-user-permissions client "myuser")

; Grant user permissions
(grant-db-user-permissions client "myuser" :read)

; Revoke user permissions
(revoke-db-user-permissions client "myuser" :read)

; Delete user
(delete-user client "myuser")
