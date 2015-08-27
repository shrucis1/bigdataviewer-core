package bdv.jogl.VolumeRenderer.gui.TFDrawPanel;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import bdv.jogl.VolumeRenderer.TransferFunctions.TransferFunction1D;

/**
 * Class handling the rescaling of the transfer function logical data while resizing 
 * @author michael
 *
 */
public class TransferFunctionWindowResizeHandler extends ComponentAdapter {

	
	private final TransferFunction1D transferFunction;
	
	public TransferFunctionWindowResizeHandler(final Dimension oldSize,
			final TransferFunction1D transferFunction) {
		
		this.transferFunction = transferFunction;
	}

	@Override
	public void componentResized(ComponentEvent e) {
		Dimension newSize = e.getComponent().getSize();
		
		transferFunction.setMaxOrdinates(new Point(newSize.width,newSize.height));
	}
}