package bdv.jogl.VolumeRenderer.ShaderPrograms.ShaderSources;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.jogamp.opengl.util.glsl.ShaderCode;

/**
 * Defines basic methods for Shader sources of a single program.
 * All shaders (Vertex, fragment, etc) should be put in here.
 * @author michael
 *
 */
public abstract class AbstractShaderSource {
	
	private List<ISourceListener> sourceListeners = new LinkedList<ISourceListener>();
	
	private int shaderLanguageVersion = 130;
	
	//default vertex shader attributes (sat)
	public static final String satPosition = "inPosition";
	
	//default vertex shader uniforms (suv = shader uniform variable)
	public static final String suvProjectionMatrix = "inProjection";

	public static final String suvViewMatrix = "inView";

	public static final String suvModelMatrix = "inModel";
	
	/**
	 * Retruns all shader codes build from the specific source.
	 * @return
	 */
	public abstract Set<ShaderCode> getShaderCodes();
	
	/**
	 * adds a source listener
	 * @param listener
	 */
	public void addSourceListener(ISourceListener listener){
		sourceListeners.add(listener);
	}
	
	/**
	 * clears all source listeners
	 */
	public void clearSourceListeners(){
		sourceListeners.clear();
	}
	
	/**
	 * @return the shaderLanguageVersion
	 */
	public int getShaderLanguageVersion() {
		return shaderLanguageVersion;
	}

	/**
	 * @param shaderLanguageVersion the shaderLanguageVersion to set
	 */
	public void setShaderLanguageVersion(int shaderLanguageVersion) {
		this.shaderLanguageVersion = shaderLanguageVersion;
		notifySourceCodeChanged();
	}
	
	/**
	 * notifies the listeners for source changes
	 */
	protected void notifySourceCodeChanged(){
		for (ISourceListener listener: sourceListeners){
			listener.sourceCodeChanged();
		}
	}
}