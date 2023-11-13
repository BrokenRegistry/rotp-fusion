/*
 * Copyright 2015-2020 Ray Fowler
 * 
 * Licensed under the GNU General Public License, Version 3 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *	 https://www.gnu.org/licenses/gpl-3.0.html
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rotp.model.galaxy;

import java.awt.Image;
import java.util.ArrayList;
import java.util.List;

import rotp.model.combat.CombatStack;
import rotp.model.empires.Empire;
import rotp.model.incidents.DiplomaticIncident;
import rotp.model.incidents.KillMonsterIncident;

public abstract class SpaceMonster extends ShipMonster implements NamedObject {
	private static final long serialVersionUID = 1L;
	protected final String nameKey;
	protected int lastAttackerId;
	private final List<Integer> path = new ArrayList<>();
	protected Float levelFactor;
	private transient List<CombatStack> combatStacks = new ArrayList<>();
	
	public Empire lastAttacker()			{ return galaxy().empire(lastAttackerId); }
	public void lastAttacker(Empire c)		{ lastAttackerId = c.id; }
	public void visitSystem(int sysId)		{ path.add(sysId); }
	public List<Integer> vistedSystems()	{ return path; }
	public int vistedSystemsCount()			{ return path.size(); }
	public List<CombatStack> combatStacks()	{
		if (combatStacks == null)
			combatStacks = new ArrayList<>();
		return combatStacks; 
	}
	public Image image()		{ return image(nameKey); }
	public void initCombat()	{ }
	public void addCombatStack(CombatStack c)	{ combatStacks.add(c); }
	public SpaceMonster(String name, int empId, Float speed, Float level)	{
		super(empId, speed);
		nameKey		= name;
		if (level == null)
			levelFactor	= 1f;
		else
			levelFactor	= level;
	}
	@Override public String name()	{ return text(nameKey);  }
	public boolean alive()			{ 
		boolean alive = false;
		for (CombatStack st: combatStacks) {
			if (!st.destroyed())
				return true;
		}
		return alive;
	}
	public void plunder() { notifyGalaxy(); }
	
	protected DiplomaticIncident killIncident(Empire emp) { return KillMonsterIncident.create(emp.id, lastAttackerId, nameKey); }
	
	private void notifyGalaxy() {
		Empire slayerEmp = lastAttacker();
		for (Empire emp: galaxy().empires()) {
			if ((emp.id != lastAttackerId) && emp.knowsOf(slayerEmp)) {
				DiplomaticIncident inc = killIncident(emp);
				emp.diplomatAI().noticeIncident(inc, slayerEmp);
			}
		}
	}
	public void degradePlanet(StarSystem sys) {}
}
