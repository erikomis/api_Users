package curso.api.rest.controller;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.Gson;

import curso.api.rest.model.Usuario;
import curso.api.rest.model.UsuarioDTO;
import curso.api.rest.repository.TelefoneRepository;
import curso.api.rest.repository.UsuarioRepository;
import curso.api.rest.service.ImplementacaoUserDetailsService;

@CrossOrigin
@RestController
@RequestMapping(value ="/usuario")
public class IndexController {
	
	@Autowired
	private UsuarioRepository usuarioRepository;
	
	@Autowired
	private ImplementacaoUserDetailsService implementacaoUserDetailsService;
	
	@Autowired
	private TelefoneRepository telefoneRepository;
	
	@GetMapping(value= "/usu/{id}", produces= "application/json")
	@CacheEvict(value = "users", allEntries = true)
	@CachePut("users")
	public ResponseEntity<UsuarioDTO> initi(@PathVariable(value = "id") Long id) {
		
		
		Optional<Usuario> usuario= usuarioRepository.findById(id);		
		return new ResponseEntity<UsuarioDTO>(new UsuarioDTO(usuario.get())  , HttpStatus.OK);
	}
	
	@GetMapping(value = "/", produces = "application/json")
	@CachePut("usuarioscache")
	public ResponseEntity<List<Usuario>> usuario() throws InterruptedException {
		List<Usuario> list = (List<Usuario>)usuarioRepository.findAll();
		
		return  new ResponseEntity<List<Usuario>>(list,HttpStatus.OK);
		
	}
	@GetMapping(value = "/{id}", produces = "application/json")
	@CachePut("usuariocache")
	public ResponseEntity<Usuario> init(@PathVariable(value = "id") long id ) throws InterruptedException {
		
		Optional<Usuario>  usuario = usuarioRepository.findById(id);
		return  new ResponseEntity<Usuario>(usuario.get(),HttpStatus.OK);
		
	}
	
	
	
	@PostMapping(value = "/", produces ="application/json")
	public ResponseEntity<Usuario> cadastrar(@RequestBody Usuario usuario) throws Exception{
		
		for(int pos = 0; pos < usuario.getTelefones().size(); pos++) {
			usuario.getTelefones().get(pos).setUsuario(usuario);
		}
		
		
		URL url = new URL("https://viacep.com.br/ws/"+usuario.getCep() +"/json/"); 
		
		URLConnection connection = url.openConnection();
		InputStream is = connection.getInputStream();
		
		BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
		
		String cep = "";
		StringBuilder jsonCep  = new StringBuilder();
		while((cep = br.readLine())!= null) {
			jsonCep.append(cep);
		}
		
		
		
		Usuario userAx = new Gson().fromJson(jsonCep.toString(), Usuario.class);
		
		usuario.setSenha(userAx.getCep());
		usuario.setLogradouro(userAx.getLogradouro());
		usuario.setComplemento(userAx.getComplemento());
		usuario.setBairro(userAx.getBairro());
		usuario.setLocalidade(userAx.getLocalidade());
		usuario.setUf(userAx.getUf());
		
		
		
		
		String senhacriptografada = new BCryptPasswordEncoder().encode(usuario.getSenha());
		usuario.setSenha(senhacriptografada);
		Usuario usuarioSalvo = usuarioRepository.save(usuario);
		
		implementacaoUserDetailsService.insereacessoPadrao(usuarioSalvo.getId());
		
		
		return new ResponseEntity<Usuario>(usuarioSalvo,HttpStatus.OK);
	}
	
	@PutMapping(value = "/", produces ="application/json")
	public ResponseEntity<Usuario> atualizar (@RequestBody Usuario usuario) throws Exception{
		
		
		for(int pos = 0; pos < usuario.getTelefones().size(); pos++) {
			usuario.getTelefones().get(pos).setUsuario(usuario);
		}
		
		Usuario userTemporario = usuarioRepository.findById(usuario.getId()).get();
		
		if(!userTemporario.getSenha().equals(usuario.getLogin())) {
			String senhacriptografada = new BCryptPasswordEncoder().encode(usuario.getSenha());
			usuario.setSenha(senhacriptografada);
		}
		
		Usuario usuarioSalvo = usuarioRepository.save(usuario);
		
		return new ResponseEntity<Usuario>(usuarioSalvo,HttpStatus.OK);
	}
	
	@DeleteMapping(value ="/{id}", produces = "application/text")
	public String  delete (@PathVariable("id")Long id) {
		usuarioRepository.deleteById(id);
		
		return "okk";
		
	}
	
	@GetMapping(value = "/usuarioPorNome/{nome}")
	public ResponseEntity<List<Usuario>> usuarioporNome(@PathVariable("nome") String nome) throws InterruptedException{
		List<Usuario> list = (List<Usuario>) usuarioRepository.findUserByNome(nome);
		
		
		return new ResponseEntity<List<Usuario>>(list, HttpStatus.OK);
	}
	
	
	@DeleteMapping(value = "/removerTelefone/{id}", produces = "application/text")
	public  String deletaTelefone(@PathVariable ("id") Long id){
		telefoneRepository.deleteById(id);
		
		return "okk";
	}
	
	
}
