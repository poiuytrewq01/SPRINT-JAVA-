package br.com.fiap.challenge.security;

import br.com.fiap.challenge.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ponte entre o Spring Security e a tabela users.
 *
 * O framework chama loadUserByUsername durante a autenticacao, compara o hash
 * devolvido com a senha digitada usando o PasswordEncoder configurado e monta
 * a sessao. Nenhuma comparacao de senha acontece aqui.
 *
 * A mensagem de erro nao diz se o problema foi o e-mail ou a senha: revelar
 * quais e-mails existem no sistema permitiria enumerar contas.
 */
@Service
@RequiredArgsConstructor
public class DatabaseUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmailIgnoreCase(email)
                .map(AuthenticatedUser::new)
                .orElseThrow(() -> new UsernameNotFoundException("Credenciais invalidas"));
    }
}
